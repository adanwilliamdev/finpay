import React from 'react';
import { useWallet } from '../../hooks/useWallet';
import {
  Grid,
  Paper,
  Typography,
  Box,
  Card,
  CardContent,
  Chip,
  Divider,
  CircularProgress,
  Button,
} from '@mui/material';
import {
  AccountBalanceWallet,
  QrCode,
  ContentCopy,
  History,
} from '@mui/icons-material';
import { formatCurrency } from '../../utils/currencyFormatter';
import { Link } from 'react-router-dom';

const WalletOverview = () => {
  const { wallet, balance, loading } = useWallet();

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="60vh">
        <CircularProgress />
      </Box>
    );
  }

  if (!wallet) {
    return (
      <Typography variant="h6" align="center">
        No wallet found. Please contact support.
      </Typography>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Wallet Overview
      </Typography>

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Box display="flex" alignItems="center" mb={2}>
              <AccountBalanceWallet sx={{ fontSize: 40, color: 'primary.main', mr: 2 }} />
              <Box>
                <Typography variant="h6">Wallet Balance</Typography>
                <Typography variant="h4" fontWeight="bold">
                  {formatCurrency(wallet.balance)}
                </Typography>
              </Box>
            </Box>

            <Divider sx={{ my: 2 }} />

            <Grid container spacing={2}>
              <Grid item xs={6}>
                <Typography variant="body2" color="textSecondary">
                  Wallet Number
                </Typography>
                <Typography variant="body1" fontWeight="medium">
                  {wallet.walletNumber}
                </Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="textSecondary">
                  Status
                </Typography>
                <Chip
                  label={wallet.status}
                  color={wallet.status === 'ACTIVE' ? 'success' : 'error'}
                  size="small"
                />
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="textSecondary">
                  Currency
                </Typography>
                <Typography variant="body1">{wallet.currency}</Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="textSecondary">
                  Created
                </Typography>
                <Typography variant="body1">
                  {new Date(wallet.createdAt).toLocaleDateString()}
                </Typography>
              </Grid>
            </Grid>
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <Paper sx={{ p: 3 }}>
                <Typography variant="h6" gutterBottom>
                  Quick Actions
                </Typography>
                <Box display="flex" gap={2} flexWrap="wrap">
                  <Button
                    variant="contained"
                    component={Link}
                    to="/transfer"
                    startIcon={<AccountBalanceWallet />}
                  >
                    Transfer
                  </Button>
                  <Button
                    variant="outlined"
                    startIcon={<History />}
                    component={Link}
                    to="/transactions"
                  >
                    History
                  </Button>
                </Box>
              </Paper>
            </Grid>

            <Grid item xs={12}>
              <Paper sx={{ p: 3 }}>
                <Typography variant="h6" gutterBottom>
                  Account Summary
                </Typography>
                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="textSecondary">
                      Total Deposits
                    </Typography>
                    <Typography variant="body1" fontWeight="medium" color="success.main">
                      {balance ? formatCurrency(balance.totalDeposits) : 'R$ 0,00'}
                    </Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="textSecondary">
                      Total Withdrawals
                    </Typography>
                    <Typography variant="body1" fontWeight="medium" color="error.main">
                      {balance ? formatCurrency(balance.totalWithdrawals) : 'R$ 0,00'}
                    </Typography>
                  </Grid>
                  <Grid item xs={12}>
                    <Typography variant="body2" color="textSecondary">
                      Total Transactions
                    </Typography>
                    <Typography variant="body1" fontWeight="medium">
                      {balance ? balance.totalTransactions : 0}
                    </Typography>
                  </Grid>
                </Grid>
              </Paper>
            </Grid>
          </Grid>
        </Grid>
      </Grid>
    </Box>
  );
};

export default WalletOverview;