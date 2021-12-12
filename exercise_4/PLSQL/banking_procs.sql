create or replace PACKAGE BODY BANKING_PROCS AS

  PROCEDURE createBankLink(
    p_bic        VARCHAR2,
    p_bankname   VARCHAR2,
    p_linkname   VARCHAR2,
    p_username   VARCHAR2,
    p_password   VARCHAR2,
    p_host       VARCHAR2,
    p_oracle_sid VARCHAR2 DEFAULT 'xe') IS

    numberOfLinks NUMBER;
	numberOfInfos NUMBER;
    link_exists EXCEPTION;

    BEGIN

      -- check if linkname already exists
      SELECT Count(*) INTO numberOfLinks FROM user_db_links WHERE DB_LINK LIKE p_linkname;
      IF (numberOfLinks > 0) THEN
        RAISE link_exists;
      END IF;
      -- create database link here
      EXECUTE IMMEDIATE 'CREATE DATABASE LINK ' || p_linkname || ' CONNECT TO ' || p_username || ' IDENTIFIED BY ' || p_password || ' USING ''(DESCRIPTION = (ADDRESS_LIST = (ADDRESS = (PROTOCOL = TCP)(HOST = ' || p_host || ' )(PORT = 1521))) (CONNECT_DATA = (SID = ' || p_oracle_sid || ')))''';


	  SELECT Count(*) INTO numberOfInfos FROM bank_config WHERE LOWER(linkname) LIKE LOWER(p_linkname);
	  IF (numberOfInfos > 0) THEN
	    -- delete old config entry
		DELETE FROM bank_config WHERE LOWER(linkname) LIKE LOWER(p_linkname);
	  END IF;
      -- store info in config table
      INSERT INTO bank_config VALUES (p_bic, p_bankname, 'remote', p_linkname, p_username, p_password, p_host, p_oracle_sid);


      EXCEPTION 
	    WHEN link_exists THEN raise_application_error(-20011, 'Error in banking_proc.createDBLink: database link with same name already exists !!');

    END createBankLink;

PROCEDURE withdraw(
    p_value NUMBER,
    p_iban  VARCHAR2,
    p_bic   VARCHAR2) IS

      unknown_account EXCEPTION;
      not_local_bic EXCEPTION;
      no_enough_credit EXCEPTION;
      BANK_NAMES_COUNT NUMBER;
      CUST_NUM_COUNT NUMBER;
      CREDIT NUMBER;
      bic_count NUMBER;
      negative_input Exception;
    BEGIN
    
        if p_bic != 'P5' then
            raise not_local_bic;
            rollback;
        end if;
--    check bic availability
        
        select count(BIC) INTO bic_count FROM BANK_CONFIG;
        if bic_count>0 THEN
            SELECT count(NAME) INTO BANK_NAMES_COUNT FROM BANK_CONFIG WHERE BIC = p_bic; --IN CASE OF BIC FOUND RETRIEVE COUNTS
            IF BANK_NAMES_COUNT = 0 THEN
                RAISE not_local_bic;
                ROLLBACK;
            END IF;
        END IF;
--        check iban in local database p5
        SELECT count(CUSTOMERNO) INTO CUST_NUM_COUNT FROM ACCOUNT WHERE IBAN = p_iban;
        IF (CUST_NUM_COUNT = 0) then
            RAISE unknown_account;
            ROLLBACK;
        END IF;
        
--        check input value is not negative
        if p_value < 0 then
            raise negative_input;
        end if;
        
--        check credit limit
        SELECT BALANCE INTO CREDIT FROM ACCOUNT WHERE IBAN like p_iban;

        IF (CREDIT < p_value) THEN
            RAISE no_enough_credit;
            ROLLBACK;
        ELSE
--          withdraw the account using update statement
            UPDATE ACCOUNT SET BALANCE = CREDIT - p_value WHERE IBAN = p_iban;
            COMMIT;
        END IF;
        
        EXCEPTION
            WHEN not_local_bic THEN RAISE_APPLICATION_ERROR(-20012, 'Error in withdraw. BIC not local');
            WHEN unknown_account THEN RAISE_APPLICATION_ERROR(-20013, 'Error in withdraw. Unknown account');
            WHEN no_enough_credit THEN RAISE_APPLICATION_ERROR(-20014, 'Error in withdraw. No enough credit');
            WHEN negative_input THEN RAISE_APPLICATION_ERROR(-20015, 'Error in withdraw. Negative input value');
            WHEN OTHERS THEN ROLLBACK;

    end withdraw;



  PROCEDURE deposit(
    p_value NUMBER,
    p_iban  VARCHAR2,
    p_bic   VARCHAR2) IS

    unknown_account EXCEPTION;
      not_local_bic EXCEPTION;
      no_enough_credit EXCEPTION;
      BANK_NAMES NUMBER;
      CUST_NUM NUMBER;
      CREDIT NUMBER;
      bic_count NUMBER;
      negative_input exception;
    BEGIN
--        check input value is not negative    
        if p_value <0 then
            raise negative_input;
        end if;
--        check whether use local or remote dbs
        IF (p_bic = 'P5') THEN
--    check bic availability        
            select count(BIC) INTO bic_count FROM BANK_CONFIG;
            if bic_count>0 THEN
            SELECT count(NAME) INTO BANK_NAMES FROM BANK_CONFIG WHERE BIC = p_bic;
            IF BANK_NAMES = 0 THEN
                RAISE not_local_bic;
                ROLLBACK;
                END IF;
            END IF;
--        check iban existence
            SELECT count(CUSTOMERNO) INTO CUST_NUM FROM ACCOUNT WHERE IBAN like p_iban;
            IF (CUST_NUM = 0) then
                RAISE unknown_account;
                ROLLBACK;
            END IF;
            
            SELECT BALANCE INTO CREDIT FROM ACCOUNT WHERE IBAN like p_iban;

            UPDATE ACCOUNT SET BALANCE = CREDIT + p_value WHERE IBAN = p_iban;
            COMMIT; --after insert or update
        
        elsIF (p_bic = 'P6') THEN
            select count(BIC) INTO bic_count FROM BANK_CONFIG@P6Link;
            if bic_count>0 THEN
            SELECT count(NAME) INTO BANK_NAMES FROM BANK_CONFIG@P6Link WHERE BIC = p_bic;
            IF BANK_NAMES = 0 THEN
                RAISE not_local_bic;
                ROLLBACK;
                END IF;
            END IF;

            SELECT count(CUSTOMERNO) INTO CUST_NUM FROM ACCOUNT@P6Link WHERE IBAN like p_iban;
            IF (CUST_NUM = 0) then
                RAISE unknown_account;
                ROLLBACK;
            END IF;
            
            SELECT BALANCE INTO CREDIT FROM ACCOUNT WHERE IBAN like p_iban;
            UPDATE ACCOUNT@P6Link SET BALANCE = CREDIT + p_value WHERE IBAN = p_iban;
            COMMIT;
        else 
            raise not_local_bic;
            rollback;
        END IF;    
        
        EXCEPTION
            WHEN not_local_bic THEN RAISE_APPLICATION_ERROR(-20012, 'Error in deposit. BIC not local');
            WHEN unknown_account THEN RAISE_APPLICATION_ERROR(-20013, 'Error in deposit. Unknown account');
            WHEN no_enough_credit THEN RAISE_APPLICATION_ERROR(-20014, 'Error in deposit. No enough credit');
            WHEN negative_input THEN RAISE_APPLICATION_ERROR(-20014, 'Error in deposit. Negative input value');
            WHEN OTHERS THEN ROLLBACK;
    END deposit;


  PROCEDURE transfer(
    p_value   NUMBER,
    from_iban VARCHAR2,
    from_bic  VARCHAR2,
    to_iban   VARCHAR2,
    to_bic    VARCHAR2
  ) IS
    
    Account1_count Number;
    Account2_count Number;
    CREDIT NUMBER;
    negative_input Exception;
    not_local_bic Exception;
    unknown_account EXCEPTION;
    no_enough_credit EXCEPTION;
    BEGIN
      -- check if negative value
        if p_value < 0 then
            raise negative_input;
        end if;
        
        if from_bic != 'P5' then
            raise not_local_bic;
            rollback;
        end if;
        
        if to_bic != 'P5' or to_bic !='P6' then
            raise not_local_bic;
            rollback;
        end if;
       -- check existence of the from iban 
        SELECT count(CUSTOMERNO) INTO Account1_count FROM ACCOUNT WHERE IBAN like from_iban;
            IF (Account1_count = 0) then
                RAISE unknown_account;
                ROLLBACK;
            END IF;
       -- check existence of the to iban 
        SELECT count(CUSTOMERNO) INTO Account2_count FROM ACCOUNT WHERE IBAN like to_iban;
            IF (Account2_count = 0) then
                SELECT count(CUSTOMERNO) INTO Account2_count FROM ACCOUNT@P6Link WHERE IBAN like to_iban;
                IF (Account2_count = 0) then
                    RAISE unknown_account;
                    ROLLBACK;
                END IF;
            END IF;

    -- check balance availability
        SELECT BALANCE INTO CREDIT FROM ACCOUNT WHERE IBAN like from_iban;
        IF (CREDIT < p_value) THEN
            RAISE no_enough_credit;
            ROLLBACK;
        END IF;
       
        withdraw(p_value, from_iban, from_bic);        
        deposit(p_value, to_iban, to_bic);
        EXCEPTION
            WHEN not_local_bic THEN RAISE_APPLICATION_ERROR(-20012, 'Error in transfer. BIC not local');
            WHEN unknown_account THEN RAISE_APPLICATION_ERROR(-20013, 'Error in transfer. Unknown account');
            WHEN no_enough_credit THEN RAISE_APPLICATION_ERROR(-20014, 'Error in transfer. No enough credit');
            WHEN negative_input THEN RAISE_APPLICATION_ERROR(-20015, 'Error in transfer. Negative input value');
            WHEN OTHERS THEN ROLLBACK;
   
    END transfer;

END BANKING_PROCS;