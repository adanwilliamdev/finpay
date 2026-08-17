import { useState, useEffect } from 'react';
import { walletService, transactionService } from '../services/auth';
import { useAuth } from './useAuth';

export const useWallet = () => {
  const { user } = useAuth();
  const [wallet, setWallet] = useState(null);
  const [balance, setBalance] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (user?.userId) {
      loadWallet();
    }
  }, [user]);

  const loadWallet = async () => {
    try {
      setLoading(true);
      const walletData = await walletService.getWalletByUserId(user.userId);
      setWallet(walletData);

      const balanceData = await transactionService.getBalanceSummary(walletData.id);
      setBalance(balanceData);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const refreshBalance = async () => {
    if (wallet?.id) {
      const balanceData = await transactionService.getBalanceSummary(wallet.id);
      setBalance(balanceData);
      return balanceData;
    }
  };

  return {
    wallet,
    balance,
    loading,
    error,
    refreshBalance,
    loadWallet
  };
};