-- Create Database Link P5
CREATE DATABASE LINK "P5Link"
   CONNECT TO "FDIS_22" IDENTIFIED BY "enNB2hy"
   USING '(DESCRIPTION =
       (ADDRESS_LIST =
         (ADDRESS = (PROTOCOL = TCP)(HOST = p5.dmi.unibas.ch)(PORT = 1521))
       )
       (CONNECT_DATA =
         (SID = xe)
       )
     )';

-- Create Database Link P6
CREATE DATABASE LINK "P6Link"
   CONNECT TO "FDIS_22" IDENTIFIED BY "enNB2hy"
   USING '(DESCRIPTION =
       (ADDRESS_LIST =
         (ADDRESS = (PROTOCOL = TCP)(HOST = p6.dmi.unibas.ch)(PORT = 1521))
       )
       (CONNECT_DATA =
         (SID = xe)
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


