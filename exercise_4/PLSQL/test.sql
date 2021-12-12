-- Execute test from p5 --

-- Local withdraw, deposit and transfer --
exec BANKING_PROCS.withdraw(40, 'CH5367A1', 'P5');
exec BANKING_PROCS.deposit(100, 'CH5367A1', 'P5');
exec BANKING_PROCS.transfer(40, 'CH5367A1', 'P5', 'CH5367A2', 'P5');

-- Remote withdraw, deposit, transfer (withdraw shouldn't work) --
exec BANKING_PROCS.withdraw(40, 'CH5367A1', 'P6');
exec BANKING_PROCS.deposit(50, 'CH5367A1', 'P6'); 
exec BANKING_PROCS.transfer(20, 'CH5367A1', 'P5', 'CH5367A1', 'P6');

-- Everything below should raise exceptions --
 
-- Transfer money from/to non-existing accounts --
exec BANKING_PROCS.transfer(50, 'CH5367A16', 'P5', 'CH5367A16', 'P5');
exec BANKING_PROCS.transfer(50, 'CH5367A16', 'P5', 'CH5367A16', 'p6');
exec BANKING_PROCS.transfer(20, 'CH5367A16', 'P5', 'CH5367A1', 'P6');
exec BANKING_PROCS.transfer(20, 'CH5367A1', 'P5', 'CH5367A16', 'P6');

-- Transfer money from/to non-existing BIC --
exec BANKING_PROCS.transfer(20, 'CH5367A1', 'P7', 'CH5367A1', 'P6');
exec BANKING_PROCS.transfer(20, 'CH5367A1', 'P5', 'CH5367A1', 'P7');

-- Deposit money to non-existing BIC --
exec BANKING_PROCS.deposit(100, 'CH5367A1', 'P7');

-- Deposit money from non-existing local account --
exec BANKING_PROCS.withdraw(40, 'CH5367A16', 'P5');

-- Withdraw value greater than the balance --
exec BANKING_PROCS.withdraw(10000000000, 'CH5367A1', 'P5');

-- Withdraw negative value from local account --
exec BANKING_PROCS.withdraw(-40, 'CH5367A1', 'P5');

-- Deposit negative value from local/remote account --
exec BANKING_PROCS.deposit(-40, 'CH5367A1', 'P5');
exec BANKING_PROCS.deposit(-40, 'CH5367A1', 'P6');
 
-- Transfer negative value from local to local/remote account --
exec BANKING_PROCS.transfer(-50, 'CH5367A1', 'P5', 'CH5367A1', 'P5');
exec BANKING_PROCS.transfer(-50, 'CH5367A1', 'P5', 'CH5367A1', 'P6');