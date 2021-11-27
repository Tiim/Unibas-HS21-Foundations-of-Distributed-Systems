
-- Create Database Link manually

CREATE DATABASE LINK "LinkNameX"
   CONNECT TO "Username" IDENTIFIED BY "password"
   USING '(DESCRIPTION =
       (ADDRESS_LIST =
         (ADDRESS = (PROTOCOL = TCP)(HOST = xxx.dmi.unibas.ch)(PORT = 1521))
       )
       (CONNECT_DATA =
         (SID = xxxOracle_SID_here_xxx)
       )
     )';


-- Only needed when setting up an Oracle server and creating users by yourself!

-- Create user stuff
create user alfredo identified by alfredos_secret;
grant connect to alfredo;
grant resource to alfredo;
grant create database link to alfredo;
grant create view to alfredo;

-- XA Recovery stuff
-- connect as sysdba
grant select on sys.dba_pending_transactions to public;
grant select on sys.pending_trans$ to public;
grant select on sys.dba_2pc_pending to public;
grant execute on sys.dbms_system to public;


