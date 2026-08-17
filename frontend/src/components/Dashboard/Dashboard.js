import React, { useEffect, useState } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { useWallet } from '../../hooks/useWallet';
import { transactionService } from '../../services/auth';
import {
  Grid,
  Paper,
  Typography,
  Box,
  Card,
  CardContent,
  CardHeader,
  Avatar,
  Button,
  CircularProgress,
} from '@mui/material';
import {
  AccountBalanceWallet,
  TrendingUp,
  TrendingDown,
  SwapHoriz,
  Refresh,
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
        <CircularProgress />
      </Box>
    );
  }

  const stats = [
    {
      title: 'Total Balance',
      value: balance?.totalBalance || 0,
      icon: <AccountBalanceWallet />,
      color: '#1976d2',
    },
    {
      title: 'Total Deposits',
      value: balance?.totalDeposits || 0,
      icon: <TrendingUp />,
      color: '#2e7d32',
    },
    {
      title: 'Total Withdrawals',
      value: balance?.totalWithdrawals || 0,
      icon: <TrendingDown />,
      color: '#d32f2f',
    },
    {
      title: 'Transactions',
      value: balance?.totalTransactions || 0,
      icon: <SwapHoriz />,
      color: '#9c27b0',
    },
  ];

  return (
    <Box sx={{ flexGrow: 1 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" gutterBottom>
          Welcome back, {user?.fullName?.split(' ')[0] || 'User'}!
        </Typography>
        <Box>
          <Button
            variant="outlined"
            startIcon={<Refresh />}
            onClick={handleRefresh}
            sx={{ mr: 2 }}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            component={Link}
            to="/transfer"
          >
            New Transfer
          </Button>
        </Box>
      </Box>

      <Grid container spacing={3}>
        {stats.map((stat, index) => (
          <Grid item xs={12} sm={6} md={3} key={index}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center" justifyContent="space-between">
                  <Box>
                    <Typography color="textSecondary" gutterBottom>
                      {stat.title}
                    </Typography>
                    <Typography variant="h5">
                      {typeof stat.value === 'number' && stat.title.includes('Balance')
                        ? formatCurrency(stat.value)
                        : typeof stat.value === 'number' && stat.title.includes('Deposits')
                        ? formatCurrency(stat.value)
                        : typeof stat.value === 'number' && stat.title.includes('Withdrawals')
                        ? formatCurrency(stat.value)
                        : stat.value}
                    </Typography>
                  </Box>
                  <Avatar sx={{ bgcolor: stat.color }}>
                    {stat.icon}
                  </Avatar>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={3} sx={{ mt: 1 }}>
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Transaction History
            </Typography>
            <TransactionChart data={chartData} />
          </Paper>
        </Grid>

        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Quick Actions
            </Typography>
            <Box display="flex" flexDirection="column" gap={2}>
              <Button
                variant="contained"
                fullWidth
                component={Link}
                to="/transfer"
              >
                Make Transfer
              </Button>
              <Button
                variant="outlined"
                fullWidth
                component={Link}
                to="/wallet"
              >
                View Wallet
              </Button>
              <Button
                variant="outlined"
                fullWidth
                component={Link}
                to="/transactions"
              >
                View All Transactions
              </Button>
            </Box>
          </Paper>
        </Grid>

        {recentTransactions.length > 0 && (
          <Grid item xs={12}>
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>
                Recent Transactions
              </Typography>
              <Box>
                {recentTransactions.map((transaction) => (
                  <Box
                    key={transaction.id}
                    sx={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      py: 1,
                      borderBottom: '1px solid #e0e0e0',
                    }}
                  >
                    <Box>
                      <Typography variant="body2" color="textSecondary">
                        {transaction.description || transaction.type}
                      </Typography>
                      <Typography variant="caption" color="textSecondary">
                        {new Date(transaction.createdAt).toLocaleString()}
                      </Typography>
                    </Box>
                    <Typography
                      variant="body1"
                      sx={{
                        color: transaction.type === 'DEPOSIT' ? '#2e7d32' :
                               transaction.type === 'WITHDRAWAL' ? '#d32f2f' : '#1976d2'
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