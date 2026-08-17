import React, { useEffect, useState } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useWallet } from '../../hooks/useWallet';
import { transactionService } from '../../services/auth';
import {
  Grid,
  Paper,
  Typography,
  Box,
  Button,
  CircularProgress,
} from '@mui/material';
import {
  AccountBalanceWalletOutlined,
  TrendingUpOutlined,
  TrendingDownOutlined,
  SwapHorizOutlined,
  Refresh,
  ArrowForward,
} from '@mui/icons-material';
import BalanceCard from './BalanceCard';
import TransactionChart from './TransactionChart';
import { formatCurrency } from '../../utils/currencyFormatter';
import { Link } from 'react-router-dom';

const Dashboard = () => {
  const { user } = useAuth();
  const { wallet, balance, loading, refreshBalance } = useWallet();
  const [recentTransactions, setRecentTransactions] = useState([]);
  const [chartData, setChartData] = useState([]);

  useEffect(() => {
    if (wallet?.id) {
      loadRecentTransactions();
    }
  }, [wallet]);

  const loadRecentTransactions = async () => {
    try {
      const data = await transactionService.getTransactions(wallet.id, 0, 5);
      setRecentTransactions(data.content || []);

      // Prepare chart data
      if (data.content) {
        const chartData = data.content.slice().reverse().map(t => ({
          date: new Date(t.createdAt).toLocaleDateString(),
          amount: t.amount,
          type: t.type
        }));
        setChartData(chartData);
      }
    } catch (error) {
      console.error('Error loading transactions:', error);
    }
  };

  const handleRefresh = async () => {
    await refreshBalance();
    await loadRecentTransactions();
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="60vh">
        <CircularProgress sx={{ color: '#2563EB' }} />
      </Box>
    );
  }

  const deposits = balance?.totalDeposits || 0;
  const withdrawals = balance?.totalWithdrawals || 0;
  const movement = deposits + withdrawals;
  const depositsShare = movement > 0 ? Math.round((deposits / movement) * 100) : null;
  const withdrawalsShare = movement > 0 ? Math.round((withdrawals / movement) * 100) : null;

  const stats = [
    {
      title: 'Total Balance',
      value: balance?.totalBalance || 0,
      icon: <AccountBalanceWalletOutlined />,
      color: '#2563EB',
      maskable: true,
      subtitle: { label: 'Available balance', value: balance?.totalBalance || 0 },
    },
    {
      title: 'Total Deposits',
      value: deposits,
      icon: <TrendingUpOutlined />,
      color: '#16A34A',
      trend: depositsShare !== null
        ? { direction: 'up', label: `${depositsShare}% of total movement` }
        : null,
    },
    {
      title: 'Total Withdrawals',
      value: withdrawals,
      icon: <TrendingDownOutlined />,
      color: '#DC2626',
      trend: withdrawalsShare !== null
        ? { direction: 'down', label: `${withdrawalsShare}% of total movement` }
        : null,
    },
    {
      title: 'Transactions',
      value: balance?.totalTransactions || 0,
      icon: <SwapHorizOutlined />,
      color: '#7C3AED',
      isCount: true,
    },
  ];

  return (
    <Box sx={{ flexGrow: 1 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3} flexWrap="wrap" gap={2}>
        <Box>
          <Typography variant="h4" sx={{ color: '#0F172A', fontSize: '1.6rem' }}>
            Welcome back, {user?.fullName?.split(' ')[0] || 'User'}!
          </Typography>
          <Typography variant="body2" sx={{ color: '#64748B', mt: 0.5 }}>
            Here's what's happening with your wallet today.
          </Typography>
        </Box>
        <Box>
          <Button
            variant="outlined"
            startIcon={<Refresh />}
            onClick={handleRefresh}
            sx={{
              mr: 1.5,
              borderColor: '#E2E8F0',
              color: '#334155',
              '&:hover': { borderColor: '#CBD5E1', backgroundColor: '#F8FAFC' },
            }}
          >
            Refresh
          </Button>
          <Button variant="contained" disableElevation component={Link} to="/transfer">
            New Transfer
          </Button>
        </Box>
      </Box>

      <Grid container spacing={2.5}>
        {stats.map((stat, index) => (
          <Grid item xs={12} sm={6} md={3} key={index}>
            <BalanceCard
              title={stat.title}
              balance={stat.value}
              icon={stat.icon}
              color={stat.color}
              maskable={stat.maskable}
              subtitle={stat.subtitle}
              trend={stat.trend}
              isCount={stat.isCount}
            />
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={2.5} sx={{ mt: 0.5 }}>
        <Grid item xs={12} md={8}>
          <Paper
            elevation={0}
            sx={{ p: 3, borderRadius: '16px', border: '1px solid #E2E8F0', height: '100%' }}
          >
            <Typography variant="h6" sx={{ color: '#0F172A', fontSize: '1.05rem', mb: 1 }}>
              Transaction History
            </Typography>
            <TransactionChart data={chartData} />
          </Paper>
        </Grid>

        <Grid item xs={12} md={4}>
          <Paper
            elevation={0}
            sx={{ p: 3, borderRadius: '16px', border: '1px solid #E2E8F0', height: '100%' }}
          >
            <Typography variant="h6" sx={{ color: '#0F172A', fontSize: '1.05rem', mb: 2 }}>
              Quick Actions
            </Typography>
            <Box display="flex" flexDirection="column" gap={1.5}>
              <Button variant="contained" disableElevation fullWidth component={Link} to="/transfer">
                Make Transfer
              </Button>
              <Button
                variant="outlined"
                fullWidth
                component={Link}
                to="/wallet"
                sx={{ borderColor: '#E2E8F0', color: '#334155' }}
              >
                View Wallet
              </Button>
              <Button
                variant="outlined"
                fullWidth
                component={Link}
                to="/transactions"
                sx={{ borderColor: '#E2E8F0', color: '#334155' }}
              >
                View All Transactions
              </Button>
            </Box>
          </Paper>
        </Grid>

        {recentTransactions.length > 0 && (
          <Grid item xs={12}>
            <Paper elevation={0} sx={{ p: 3, borderRadius: '16px', border: '1px solid #E2E8F0' }}>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={1.5}>
                <Typography variant="h6" sx={{ color: '#0F172A', fontSize: '1.05rem' }}>
                  Recent Transactions
                </Typography>
                <Button
                  component={Link}
                  to="/transactions"
                  size="small"
                  endIcon={<ArrowForward sx={{ fontSize: 16 }} />}
                  sx={{ color: '#2563EB', fontSize: '0.82rem' }}
                >
                  View all
                </Button>
              </Box>
              <Box>
                {recentTransactions.map((transaction) => (
                  <Box
                    key={transaction.id}
                    sx={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      py: 1.5,
                      borderBottom: '1px solid #F1F5F9',
                      '&:last-of-type': { borderBottom: 'none' },
                    }}
                  >
                    <Box>
                      <Typography variant="body2" sx={{ color: '#0F172A', fontWeight: 600 }}>
                        {transaction.description || transaction.type}
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#94A3B8' }}>
                        {new Date(transaction.createdAt).toLocaleString()}
                      </Typography>
                    </Box>
                    <Typography
                      variant="body1"
                      sx={{
                        fontWeight: 700,
                        color: transaction.type === 'DEPOSIT' ? '#16A34A' :
                               transaction.type === 'WITHDRAWAL' ? '#DC2626' : '#2563EB'
                      }}
                    >
                      {transaction.type === 'DEPOSIT' ? '+' :
                       transaction.type === 'WITHDRAWAL' ? '-' : ''}
                      {formatCurrency(transaction.amount)}
                    </Typography>
                  </Box>
                ))}
              </Box>
            </Paper>
          </Grid>
        )}
      </Grid>
    </Box>
  );
};

export default Dashboard;
