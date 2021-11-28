exec BANKING_PROCS.withdraw(40, 'CH5367A1', 'P5');

exec BANKING_PROCS.deposit(100, 'CH5367A1', 'P5');

exec BANKING_PROCS.transfer(40, 'CH5367A1', 'P5', 'CH5367A2', 'P5');

exec BANKING_PROCS.withdraw(40, 'CH5367A1', 'P6');

exec BANKING_PROCS.deposit(50, 'CH5367A1', 'P6'); 

exec BANKING_PROCS.transfer(20, 'CH5367A1', 'P5', 'CH5367A1', 'P6');

exec BANKING_PROCS.transfer(50, 'CH5367A16', 'P5', 'CH5367A16', 'P5');

exec BANKING_PROCS.transfer(50, 'CH5367A16', 'P5', 'CH5367A16', 'p6');
exec BANKING_PROCS.deposit(100, 'CH5367A1', 'P7');

exec BANKING_PROCS.withdraw(40, 'CH5367A16', 'P5');
exec BANKING_PROCS.withdraw(40, 'CH5367A1', 'P6');

exec BANKING_PROCS.transfer(20, 'CH5367A16', 'P5', 'CH5367A1', 'P6');
exec BANKING_PROCS.transfer(20, 'CH5367A1', 'P5', 'CH5367A16', 'P6');

exec BANKING_PROCS.transfer(20, 'CH5367A1', 'P7', 'CH5367A16', 'P6');
exec BANKING_PROCS.transfer(20, 'CH5367A1', 'P5', 'CH5367A16', 'P7');

exec BANKING_PROCS.withdraw(-40, 'CH5367A1', 'P5');

exec BANKING_PROCS.deposit(-40, 'CH5367A1', 'P5');
exec BANKING_PROCS.deposit(-40, 'CH5367A1', 'P6');
 
exec BANKING_PROCS.transfer(-50, 'CH5367A1', 'P5', 'CH5367A1', 'P5');
exec BANKING_PROCS.transfer(-50, 'CH5367A1', 'P5', 'CH5367A1', 'P6');

