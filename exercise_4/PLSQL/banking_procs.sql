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

--removes value from iban in bic
  PROCEDURE withdraw(
    p_value NUMBER,
    p_iban  VARCHAR2,
    p_bic   VARCHAR2) IS

      unknown_account EXCEPTION;
      not_local_bic EXCEPTION;
      no_enough_credit EXCEPTION;
      BANK_NAME VARCHAR2(50);
      CUST_NUM VARCHAR2(50);
      CREDIT NUMBER;
    BEGIN
    
        SELECT NAME INTO BANK_NAME FROM BANK_CONFIG WHERE BIC = p_bic;
        IF BANK_NAME IS NULL THEN
            RAISE not_local_bic;
        END IF;
        
        SELECT CUSTOMERNO, BALANCE INTO CUST_NUM, CREDIT FROM ACCOUNT WHERE IBAN = p_iban;
        IF CUST_NUM IS NULL THEN
            RAISE unknown_account;
            ROLLBACK;
        END IF;
        
        IF (CREDIT < p_value) THEN
            RAISE no_enough_credit;
            ROLLBACK;
        ELSE
            UPDATE ACCOUNT SET BALANCE = CREDIT - p_value WHERE IBAN = p_iban;
            COMMIT;
        END IF;
        
        EXCEPTION
            WHEN not_local_bic THEN RAISE_APPLICATION_ERROR(-20001, 'Error in withdraw. BIC Not Local');
            WHEN unknown_account THEN RAISE_APPLICATION_ERROR(-20002, 'Error in withdraw. Unknown account');
            WHEN no_enough_credit THEN RETURN;
            WHEN OTHERS THEN ROLLBACK;
      -- TODO put your PL/SQL Code here to withdraw money from the local bank
      dbms_output.put_line('not ready yet :-(');


    END withdraw;

  PROCEDURE deposit(
    p_value NUMBER,
    p_iban  VARCHAR2,
    p_bic   VARCHAR2) IS

    unknown_account EXCEPTION;
      not_local_bic EXCEPTION;
      no_enough_credit EXCEPTION;
      BANK_NAME VARCHAR2(50);
      CUST_NUM VARCHAR2(50);
      CREDIT NUMBER;
    BEGIN
        IF (p_bic = 'P5') THEN
            
            SELECT NAME INTO BANK_NAME FROM BANK_CONFIG WHERE BIC = p_bic;
            IF BANK_NAME IS NULL THEN
                RAISE not_local_bic;
            END IF;
            
            SELECT CUSTOMERNO, BALANCE INTO CUST_NUM, CREDIT FROM ACCOUNT WHERE IBAN = p_iban;
            IF CUST_NUM IS NULL THEN
                RAISE unknown_account;
            END IF;
            
            
            UPDATE ACCOUNT SET BALANCE = CREDIT + p_value WHERE IBAN = p_iban;
            COMMIT; --after insert or update
        
        elsIF (p_bic = 'P6') THEN
            SELECT NAME INTO BANK_NAME FROM BANK_CONFIG@P6Link WHERE BIC = p_bic;
            IF BANK_NAME IS NULL THEN
                RAISE not_local_bic;
            END IF;
            
            SELECT CUSTOMERNO, BALANCE INTO CUST_NUM, CREDIT FROM ACCOUNT@P6Link WHERE IBAN = p_iban;
            IF CUST_NUM IS NULL THEN
                RAISE unknown_account;
            END IF;
            
            
            UPDATE ACCOUNT@P6Link SET BALANCE = CREDIT + p_value WHERE IBAN = p_iban;
            COMMIT; --after insert or update
        END IF;    
        
        EXCEPTION
            WHEN not_local_bic THEN dbms_output.put_line('This bank is invalid');
            WHEN unknown_account THEN RETURN;
            WHEN no_enough_credit THEN RETURN;
            WHEN OTHERS THEN ROLLBACK;
       

      -- TODO put your PL/SQL Code here to deposit money on the local bank
      dbms_output.put_line('not ready yet :-(');

    END deposit;


  PROCEDURE transfer(
    p_value   NUMBER,
    from_iban VARCHAR2,
    from_bic  VARCHAR2,
    to_iban   VARCHAR2,
    to_bic    VARCHAR2
  ) IS
    bank_not_exist EXCEPTION;
    account_not_exist EXCEPTION;
    credit_not_enough EXCEPTION;
    BEGIN
       
--        createBankLink(to_bic, 'remote_bank', 'remote_bank_link', 'fdis_22', 'enNB2hy', 'p6.dmi.unibas.ch');    
        withdraw(p_value, from_iban, from_bic);        
        deposit(p_value, to_iban, to_bic);
--        deposit@p6.dmi.unibas.ch(p_value, to_iban, to_bic);
        EXCEPTION
            WHEN bank_not_exist THEN RETURN;
            WHEN account_not_exist THEN RETURN;
            WHEN credit_not_enough THEN RETURN;
            WHEN OTHERS THEN ROLLBACK;
      -- TODO put your PL/SQL Code here to transfer money from the local bank to an remote bank.
      -- Attention: use the previously defined deposit and withdraw PL/SQL procedures again here.
      -- You can also use PL/SQL procedures via the database link at the remote bank.
      dbms_output.put_line('not ready yet :-(');

    END transfer;

END BANKING_PROCS;