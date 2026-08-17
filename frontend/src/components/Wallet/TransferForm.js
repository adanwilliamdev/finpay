import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useWallet } from '../../hooks/useWallet';
import { transactionService } from '../../services/auth';
import {
  Paper,
  Typography,
  Box,
  TextField,
  Button,
  Alert,
  CircularProgress,
  Stepper,
  Step,
  StepLabel,
  Card,
  CardContent,
} from '@mui/material';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import CurrencyInput from 'react-currency-input-field';
import { toast } from 'react-toastify';
import { formatCurrency } from '../../utils/currencyFormatter';

const schema = yup.object({
  destinationWalletId: yup.string().required('Destination wallet ID is required'),
  amount: yup.number()
    .required('Amount is required')
    .positive('Amount must be positive')
    .min(0.01, 'Minimum amount is R$ 0,01'),
  description: yup.string().max(500, 'Description max 500 characters'),
}).required();

const TransferForm = () => {
  const navigate = useNavigate();
  const { wallet, refreshBalance } = useWallet();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [activeStep, setActiveStep] = useState(0);

  const { register, handleSubmit, formState: { errors }, setValue, watch } = useForm({
    resolver: yupResolver(schema),
    defaultValues: {
      destinationWalletId: '',
      amount: '',
      description: '',
    }
  });

  const amount = watch('amount');

  const onSubmit = async (data) => {
    if (!wallet) {
      toast.error('Wallet not found');
      return;
    }

    try {
      setLoading(true);
      setError('');

      const transferData = {
        sourceWalletId: wallet.id,
        destinationWalletId: data.destinationWalletId,
        amount: parseFloat(data.amount),
        description: data.description || 'Transfer',
      };

      await transactionService.transfer(transferData);
      setSuccess(true);
      setActiveStep(1);
      await refreshBalance();
      toast.success('Transfer completed successfully!');

      setTimeout(() => {
        navigate('/');
      }, 3000);
    } catch (err) {
      setError(err.response?.data?.message || 'Transfer failed. Please try again.');
      toast.error('Transfer failed');
    } finally {
      setLoading(false);
    }
  };

  const steps = ['Enter Transfer Details', 'Confirm Transfer'];

  return (
    <Box sx={{ maxWidth: 600, mx: 'auto' }}>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h5" gutterBottom>
          Make a Transfer
        </Typography>

        <Stepper activeStep={activeStep} sx={{ mb: 3 }}>
          {steps.map((label) => (
            <Step key={label}>
              <StepLabel>{label}</StepLabel>
            </Step>
          ))}
        </Stepper>

        {success ? (
          <Box textAlign="center" py={4}>
            <Typography variant="h6" color="success.main" gutterBottom>
              Transfer Successful!
            </Typography>
            <Typography variant="body1" color="textSecondary">
              Your transfer of {formatCurrency(parseFloat(amount))} has been completed.
            </Typography>
            <Typography variant="body2" color="textSecondary" sx={{ mt: 2 }}>
              Redirecting to dashboard...
            </Typography>
          </Box>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)}>
            {error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {error}
              </Alert>
            )}

            <Card sx={{ mb: 3, bgcolor: 'grey.50' }}>
              <CardContent>
                <Typography variant="body2" color="textSecondary" gutterBottom>
                  From Wallet
                </Typography>
                <Typography variant="body1" fontWeight="medium">
                  {wallet?.walletNumber || 'N/A'}
                </Typography>
                <Typography variant="body2" color="textSecondary">
                  Balance: {formatCurrency(wallet?.balance || 0)}
                </Typography>
              </CardContent>
            </Card>

            <TextField
              fullWidth
              label="Destination Wallet ID"
              margin="normal"
              {...register('destinationWalletId')}
              error={!!errors.destinationWalletId}
              helperText={errors.destinationWalletId?.message}
              placeholder="Enter the destination wallet ID"
            />

            <Box sx={{ mt: 2 }}>
              <Typography variant="body2" color="textSecondary" gutterBottom>
                Amount (R$)
              </Typography>
              <CurrencyInput
                id="amount"
                name="amount"
                className="currency-input"
                placeholder="R$ 0,00"
                decimalsLimit={2}
                onValueChange={(value) => setValue('amount', value)}
                style={{
                  width: '100%',
                  padding: '12px',
                  fontSize: '24px',
                  border: '1px solid #ced4da',
                  borderRadius: '4px',
                }}
              />
              {errors.amount && (
                <Typography variant="caption" color="error">
                  {errors.amount.message}
                </Typography>
              )}
            </Box>

            <TextField
              fullWidth
              label="Description (Optional)"
              margin="normal"
              multiline
              rows={2}
              {...register('description')}
              error={!!errors.description}
              helperText={errors.description?.message}
              placeholder="Transfer description"
            />

            <Box sx={{ mt: 3, display: 'flex', gap: 2 }}>
              <Button
                variant="outlined"
                fullWidth
                onClick={() => navigate('/')}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                variant="contained"
                fullWidth
                disabled={loading}
              >
                {loading ? <CircularProgress size={24} /> : 'Transfer'}
              </Button>
            </Box>
          </form>
        )}
      </Paper>
    </Box>
  );
};

export default TransferForm;