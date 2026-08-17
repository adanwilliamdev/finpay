// frontend/src/components/Transactions/TransactionList.js - VERSÃO ATUALIZADA
import React, { useState, useEffect } from 'react';
import { useWallet } from '../../hooks/useWallet';
import { transactionService } from '../../services/auth';
import {
  Paper,
  Typography,
  Box,
  TablePagination,
  Chip,
  IconButton,
  CircularProgress,
  TextField,
  InputAdornment,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Grid,
  ToggleButton,
  ToggleButtonGroup,
} from '@mui/material';
import {
  Search,
  Refresh,
  ViewList,
  ViewModule,
} from '@mui/icons-material';
import TransactionItem from './TransactionItem';
import { toast } from 'react-toastify';

const TransactionList = () => {
  const { wallet, refreshBalance } = useWallet();
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [total, setTotal] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('ALL');
  const [viewMode, setViewMode] = useState('list');

  useEffect(() => {
    if (wallet?.id) {
      loadTransactions();
    }
  }, [wallet, page, rowsPerPage, filterType]);

  const loadTransactions = async () => {
    try {
      setLoading(true);
      const data = await transactionService.getTransactions(
        wallet.id,
        page,
        rowsPerPage
      );
      setTransactions(data.content || []);
      setTotal(data.totalElements || 0);
    } catch (error) {
      console.error('Error loading transactions:', error);
      toast.error('Failed to load transactions');
    } finally {
      setLoading(false);
    }
  };

  const handleChangePage = (event, newPage) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const handleViewModeChange = (event, newMode) => {
    if (newMode !== null) {
      setViewMode(newMode);
    }
  };

  const handleReverseTransaction = async (transactionId) => {
    try {
      await transactionService.reverseTransaction(transactionId);
      toast.success('Transaction reversed successfully!');
      await loadTransactions();
      await refreshBalance();
    } catch (error) {
      console.error('Error reversing transaction:', error);
      toast.error(error.response?.data?.message || 'Failed to reverse transaction');
    }
  };

  const filteredTransactions = transactions.filter(transaction => {
    if (filterType !== 'ALL' && transaction.type !== filterType) {
      return false;
    }
    if (searchTerm) {
      const search = searchTerm.toLowerCase();
      return (
        transaction.transactionId.toLowerCase().includes(search) ||
        transaction.description?.toLowerCase().includes(search)
      );
    }
    return true;
  });

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="60vh">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">
          Transaction History
        </Typography>
        <Box display="flex" gap={1}>
          <ToggleButtonGroup
            value={viewMode}
            exclusive
            onChange={handleViewModeChange}
            size="small"
          >
            <ToggleButton value="list">
              <ViewList />
            </ToggleButton>
            <ToggleButton value="grid">
              <ViewModule />
            </ToggleButton>
          </ToggleButtonGroup>
          <IconButton onClick={loadTransactions}>
            <Refresh />
          </IconButton>
        </Box>
      </Box>

      <Paper sx={{ p: 2, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              placeholder="Search transactions..."
              size="small"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Search />
                  </InputAdornment>
                ),
              }}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <FormControl fullWidth size="small">
              <InputLabel>Type</InputLabel>
              <Select
                value={filterType}
                onChange={(e) => setFilterType(e.target.value)}
                label="Type"
              >
                <MenuItem value="ALL">All Types</MenuItem>
                <MenuItem value="DEPOSIT">Deposits</MenuItem>
                <MenuItem value="WITHDRAWAL">Withdrawals</MenuItem>
                <MenuItem value="TRANSFER">Transfers</MenuItem>
                <MenuItem value="REVERSAL">Reversals</MenuItem>
              </Select>
            </FormControl>
          </Grid>
        </Grid>
      </Paper>

      {filteredTransactions.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="textSecondary">
            No transactions found
          </Typography>
        </Paper>
      ) : (
        <Box>
          {viewMode === 'list' ? (
            <Box>
              {filteredTransactions.map((transaction) => (
                <TransactionItem
                  key={transaction.id}
                  transaction={transaction}
                  onReverse={handleReverseTransaction}
                />
              ))}
            </Box>
          ) : (
            <Grid container spacing={2}>
              {filteredTransactions.map((transaction) => (
                <Grid item xs={12} sm={6} md={4} key={transaction.id}>
                  <TransactionItem
                    transaction={transaction}
                    onReverse={handleReverseTransaction}
                  />
                </Grid>
              ))}
            </Grid>
          )}
        </Box>
      )}

      <TablePagination
        rowsPerPageOptions={[5, 10, 25, 50]}
        component="div"
        count={total}
        rowsPerPage={rowsPerPage}
        page={page}
        onPageChange={handleChangePage}
        onRowsPerPageChange={handleChangeRowsPerPage}
        labelRowsPerPage="Transactions per page:"
      />
    </Box>
  );
};

export default TransactionList;