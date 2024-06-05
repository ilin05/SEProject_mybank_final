package com.mybank.module1_counter.service;

import com.mybank.module1_counter.entities.FixedDeposit;
import com.mybank.module1_counter.request.FreezeInfo;
import com.mybank.module1_counter.entities.SavingAccount;
import com.mybank.module1_counter.entities.TransactionInfo;
import com.mybank.module1_counter.mapper.CashierDutyMapper;
import com.mybank.module1_counter.request.*;
import com.mybank.utils.ApiResult;
import com.mybank.utils.HashUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CashierDutyServiceImpl implements CashierDutyService {
    @Autowired
    private CashierDutyMapper cashierDutyMapper;


    @Override
    public ApiResult getAccountInfo(String accountId) {
        SavingAccount account=cashierDutyMapper.selectAccount(accountId);
        if(account==null) return ApiResult.failure("not exists");
        if(account.getDeleted()) return ApiResult.failure("The card is already deleted");
        return ApiResult.success(account);
    }

    @Override
    @Transactional
    public ApiResult demandDeposit(String accountId, String password, Double amount) {
        try{
            if(cashierDutyMapper.isDelete(accountId)) return ApiResult.failure("The card is already deleted");
            if(cashierDutyMapper.isFrozen(accountId)) return ApiResult.failure("The card is now frozen");
            if(cashierDutyMapper.isLost(accountId)) return ApiResult.failure("The card is now lost");

            String hashPassword = HashUtils.md5Hash(password);
            int ok = cashierDutyMapper.judgePassword(accountId, hashPassword);
            if(ok != 1) return ApiResult.failure("Password Error!");

            TransactionInfo txn = new TransactionInfo();
            txn.setTransactionId(1);
            txn.setCardId(accountId);
            txn.setMoneyGoes(accountId);
            txn.setTransactionTime(LocalDateTime.now());
            txn.setTransactionType("demand_deposit");
            txn.setTransactionAmount(amount);
            txn.setCurrency("CNY");
            txn.setCardType("save");
            txn.setTransactionChannel("cashier");

            cashierDutyMapper.updateAccountBalance(accountId, amount);
            cashierDutyMapper.insertTransaction(txn);
            return ApiResult.success(txn);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResult.failure("Error demand deposit");
        }
    }

    @Override
    @Transactional
    public ApiResult fixedDeposit(String accountId, String password, String depositType, Double amount) {
        try{
            if(cashierDutyMapper.isDelete(accountId)) return ApiResult.failure("The card is already deleted");
            if(cashierDutyMapper.isFrozen(accountId)) return ApiResult.failure("The card is now frozen");
            if(cashierDutyMapper.isLost(accountId)) return ApiResult.failure("The card is now lost");

            String hashPassword = HashUtils.md5Hash(password);
            int ok = cashierDutyMapper.judgePassword(accountId, hashPassword);
            if(ok != 1) return ApiResult.failure("Password Error!");

            FixedDeposit fixedDeposit = new FixedDeposit();
            fixedDeposit.setDepositTime(LocalDateTime.now());
            fixedDeposit.setDepositAmount(amount);
            fixedDeposit.setAccountId(accountId);
            fixedDeposit.setDepositType(depositType);
            System.out.println(fixedDeposit);
            cashierDutyMapper.insertFixedDeposit(fixedDeposit);
            return ApiResult.success(fixedDeposit);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResult.failure("Error fixed deposit");
        }
    }

    @Override
    @Transactional
    public ApiResult showFixedDeposit(String accountId) {
        try{
            if(cashierDutyMapper.isDelete(accountId)) return ApiResult.failure("The card is already deleted");
            List<FixedDeposit> fixedDeposits = cashierDutyMapper.showFixedDeposit(accountId);
            return ApiResult.success(fixedDeposits);
        } catch (Exception e){
            return ApiResult.failure("Error showDemandDeposit");
        }
    }


    @Override
    @Transactional
    public ApiResult transfer(TransferRequest txnRequest) {
        try{
            String accountId = txnRequest.getCardId();
            if(cashierDutyMapper.isDelete(accountId)) return ApiResult.failure("The card is already deleted");
            if(cashierDutyMapper.isFrozen(accountId)) return ApiResult.failure("The card is now frozen");
            if(cashierDutyMapper.isLost(accountId)) return ApiResult.failure("The card is now lost");

            String payeeId = txnRequest.getMoneyGoes();
            if(cashierDutyMapper.isDelete(payeeId)) return ApiResult.failure("The card is already deleted");
            if(cashierDutyMapper.isFrozen(payeeId)) return ApiResult.failure("The card is now frozen");
            if(cashierDutyMapper.isLost(payeeId)) return ApiResult.failure("The card is now lost");

            String hashPassword = HashUtils.md5Hash(txnRequest.getPassword());
            int ok = cashierDutyMapper.judgePassword(txnRequest.getCardId(), hashPassword);
            if(ok != 1) return ApiResult.failure("Password Error!");

            LocalDateTime now = LocalDateTime.now();
            TransactionInfo txn1 = new TransactionInfo();
            TransactionInfo txn2 = new TransactionInfo();
            txn1.setCardId(txnRequest.getCardId());
            txn2.setCardId(txnRequest.getMoneyGoes());
            txn1.setTransactionTime(now);
            txn2.setTransactionTime(now);
            txn1.setTransactionType("transaction");
            txn2.setTransactionType("transaction");
            txn1.setTransactionAmount(txnRequest.getTransactionAmount());
            txn2.setTransactionAmount(txnRequest.getTransactionAmount());
            txn1.setMoneySource(txnRequest.getCardId());
            txn2.setMoneySource(txnRequest.getCardId());
            txn1.setMoneyGoes(txnRequest.getMoneyGoes());
            txn2.setMoneyGoes(txnRequest.getMoneyGoes());
            txn1.setCurrency("CNY");
            txn1.setCardType("save");
            txn1.setTransactionChannel("cashier");
            txn2.setCurrency("CNY");
            txn2.setCardType("save");
            txn2.setTransactionChannel("cashier");
            cashierDutyMapper.insertTransaction(txn1);
            cashierDutyMapper.insertTransaction(txn2);
            cashierDutyMapper.updateAccountBalance(txnRequest.getCardId(), -txnRequest.getTransactionAmount());
            cashierDutyMapper.updateAccountBalance(txnRequest.getMoneyGoes(), txnRequest.getTransactionAmount());
            return ApiResult.success(txn1);

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResult.failure("Error transaction");
        }
    }


    @Override
    @Transactional
    public ApiResult withdrawDemandMoney(String accountId, String password, Double amount) {
        try{
            if(cashierDutyMapper.isDelete(accountId)) return ApiResult.failure("The card is already deleted");
            if(cashierDutyMapper.isFrozen(accountId)) return ApiResult.failure("The card is now frozen");
            if(cashierDutyMapper.isLost(accountId)) return ApiResult.failure("The card is now lost");

            String hashPassword = HashUtils.md5Hash(password);
            int ok = cashierDutyMapper.judgePassword(accountId, hashPassword);
            if(ok != 1) return ApiResult.failure("Password Error!");

            TransactionInfo txn = new TransactionInfo();
            txn.setCardId(accountId);
            txn.setTransactionTime(LocalDateTime.now());
            txn.setTransactionType("withdrawDemand");
            txn.setTransactionAmount(amount);
            txn.setMoneySource(accountId);
            txn.setCurrency("CNY");
            txn.setCardType("save");
            txn.setTransactionChannel("cashier");
            cashierDutyMapper.updateAccountBalance(accountId, -amount);
            cashierDutyMapper.insertTransaction(txn);
            return ApiResult.success(txn);

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResult.failure("Error withdrawDemandMoney");
        }
    }

    @Override
    @Transactional
    public ApiResult withdrawFixedMoney(int fixedDepositId, String accountId, String password, Double amount) {
        try{
            if(cashierDutyMapper.isDelete(accountId)) return ApiResult.failure("The card is already deleted");
            if(cashierDutyMapper.isFrozen(accountId)) return ApiResult.failure("The card is now frozen");
            if(cashierDutyMapper.isLost(accountId)) return ApiResult.failure("The card is now lost");

            String hashPassword = HashUtils.md5Hash(password);
            int ok = cashierDutyMapper.judgePassword(accountId, hashPassword);
            if(ok != 1) return ApiResult.failure("Password Error!");

            TransactionInfo txn = new TransactionInfo();
            txn.setTransactionId(5);
            txn.setCardId(accountId);
            txn.setTransactionTime(LocalDateTime.now());
            txn.setTransactionType("withdrawFixedDeposit");
            txn.setMoneySource(accountId);
            txn.setCurrency("CNY");
            txn.setCardType("save");
            txn.setTransactionChannel("cashier");
            Double transferAmount = cashierDutyMapper.getFixedDepositAmount(fixedDepositId) - amount;
            txn.setTransactionAmount(transferAmount);
            cashierDutyMapper.insertTransaction(txn);
            cashierDutyMapper.updateAccountBalance(accountId, transferAmount);
            cashierDutyMapper.deleteFixedDeposit(fixedDepositId);
            return ApiResult.success(txn);

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResult.failure("Error withdraw fixed deposit");
        }
    }



    @Override
    @Transactional
    public ApiResult openAccount(SavingAccount account) {
        try {
            Integer customerId=cashierDutyMapper.selectCustomer(account.getIdNumber());
            if(customerId == null) cashierDutyMapper.insertCustomer(account);

            Integer updatedCustomerId = cashierDutyMapper.selectCustomer(account.getIdNumber());

            int count=cashierDutyMapper.selectAccountCount(updatedCustomerId);
            if(count >= 99) throw new RuntimeException("exceed account limit");

            String accountId="628888"+ String.format("%011d",updatedCustomerId)+String.format("%02d",count+1);
            account.setOpenTime(LocalDateTime.now());
            account.setAccountId(accountId);
            account.setCustomerId(updatedCustomerId);
            account.setBalance(account.getOpenAmount());
            account.setPassword(HashUtils.md5Hash(account.getPassword()));
            cashierDutyMapper.insertAccount(account);
            SavingAccount newAccount = cashierDutyMapper.selectAccount(accountId);
            return ApiResult.success(newAccount);

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResult.failure(e.getMessage());
        }
    }


    @Override
    @Transactional
    public ApiResult freeze(FreezeInfo freezeInfo) {
        try{
            SavingAccount account=cashierDutyMapper.selectAccount(freezeInfo.getAccountId());
            if(account==null) return ApiResult.failure("not exists");
            if(account.getDeleted()) return ApiResult.failure("The card is already deleted");

            if(cashierDutyMapper.isFrozen(freezeInfo.getAccountId())) return ApiResult.failure("already frozen");

            cashierDutyMapper.changeFreezeState(freezeInfo.getAccountId(),true);
            cashierDutyMapper.insertFreezeRecord(freezeInfo.getAccountId(),LocalDateTime.now(),freezeInfo.getUnfreezeTime(),freezeInfo.getReason());
            return ApiResult.success(null);

        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResult.failure(e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResult unfreeze(String accountId) {
        try{
            SavingAccount account=cashierDutyMapper.selectAccount(accountId);
            if(account==null) return ApiResult.failure("not exists");
            if(account.getDeleted()) return ApiResult.failure("The card is already deleted");

            if(!cashierDutyMapper.isFrozen(accountId)) return ApiResult.failure("not frozen");

            cashierDutyMapper.changeFreezeState(accountId,false);
            int freezeStateRecordId=cashierDutyMapper.selectFreezeRecord(accountId);
            cashierDutyMapper.changeFreezeRecord(freezeStateRecordId,LocalDateTime.now());
            return ApiResult.success(null);
        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return  ApiResult.failure(e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResult reportLoss(String accountId) {
        try{
            SavingAccount account=cashierDutyMapper.selectAccount(accountId);
            if(account==null) return ApiResult.failure("not exists");
            if(account.getDeleted()) return ApiResult.failure("The card is already deleted");

            if(cashierDutyMapper.isLost(accountId)) return ApiResult.failure("already lost");

            cashierDutyMapper.changeLossState(accountId,true);
            cashierDutyMapper.insertLossRecord(accountId,LocalDateTime.now());
            return ApiResult.success(null);
        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResult.failure(e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResult reissue(String accountId) {
        try{
            SavingAccount account=cashierDutyMapper.selectAccount(accountId);
            if(account==null) return ApiResult.failure("not exists");
            if(account.getDeleted()) return ApiResult.failure("The card is already deleted");

            if(!cashierDutyMapper.isLost(accountId)) return ApiResult.failure("not lost");
            cashierDutyMapper.changeLossState(accountId,false);
            int lossStateRecordId=cashierDutyMapper.selectLossRecord(accountId);
            cashierDutyMapper.changeLossRecord(lossStateRecordId,LocalDateTime.now());
            return ApiResult.success(null);
        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResult.failure(e.getMessage());
        }
    }
    @Override
    @Transactional
    public ApiResult closeAccount(String accountId, String password, String idNumber){
        try{
            SavingAccount account=cashierDutyMapper.selectAccount(accountId);
            if(account==null) return ApiResult.failure("not exists");

            if(account.getDeleted()) return ApiResult.failure("The card is already deleted");
            if(account.getFreezeState()) return ApiResult.failure("The card is now frozen");
            if(account.getLossState()) return ApiResult.failure("The card is now lost");

            String hashedPassword = HashUtils.md5Hash(password);
            if(!account.getIdNumber().equals(idNumber)) return ApiResult.failure("The idNumber dose not match account_id");
            if(cashierDutyMapper.judgePassword(accountId,hashedPassword)==0){
                return ApiResult.failure("password is wrong");
            }
            cashierDutyMapper.changeDeleteState(accountId,true);
            return ApiResult.success(null);

        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResult.failure(e.getMessage());
        }
    }
    @Override
    @Transactional
    public ApiResult modifyAccountPassword(String accountId, String oldPassword, String newPassword){
        try{
            SavingAccount account=cashierDutyMapper.selectAccount(accountId);
            if(account==null) return ApiResult.failure("not exists");
            if(account.getDeleted()) return ApiResult.failure("The card is already deleted");

            if(cashierDutyMapper.isFrozen(accountId)) return ApiResult.failure("The card is now frozen");
            if(cashierDutyMapper.isLost(accountId)) return ApiResult.failure("The card is now lost");

            String hashedPassword = HashUtils.md5Hash(oldPassword);
            if(cashierDutyMapper.judgePassword(accountId,hashedPassword)==0){
                return ApiResult.failure(" old password is wrong");
            }

            cashierDutyMapper.changePassword(accountId,HashUtils.md5Hash(newPassword));
            return ApiResult.success(null);

        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResult.failure(e.getMessage());
        }
    }

}
