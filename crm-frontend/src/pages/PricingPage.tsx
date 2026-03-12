/* ============================================================
   PricingPage – Displays plans, current plan, and upgrade options
   ============================================================ */
import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Button,
  Grid,
  Chip,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Alert,
  Snackbar,
  Divider,
} from '@mui/material';
import {
  Check as CheckIcon,
  Close as CloseIcon,
  Star as StarIcon,
  Rocket as RocketIcon,
  WorkspacePremium as PremiumIcon,
  Diamond as DiamondIcon,
} from '@mui/icons-material';
import api from '../services/api';
import type { ApiResponse, TenantPlan } from '../types';

interface PlanDef {
  name: string;
  price: string;
  priceDetail: string;
  icon: React.ReactNode;
  color: string;
  features: { label: string; included: boolean; detail?: string }[];
  popular?: boolean;
}

const PLANS: PlanDef[] = [
  {
    name: 'FREE',
    price: '$0',
    priceDetail: 'forever',
    icon: <StarIcon />,
    color: '#9e9e9e',
    features: [
      { label: 'Up to 3 users', included: true },
      { label: '2 custom objects', included: true },
      { label: '3 workflows', included: true },
      { label: '1 dashboard', included: true },
      { label: '100 records per object', included: true },
      { label: 'AI Config (plain English setup)', included: true },
      { label: 'AI Insights & Predictions', included: false },
      { label: 'Email open/click tracking', included: false },
      { label: 'REST API access', included: false },
      { label: 'Integrations (Zapier, Slack)', included: false },
    ],
  },
  {
    name: 'STARTER',
    price: '$29',
    priceDetail: 'per user / month',
    icon: <RocketIcon />,
    color: '#1976d2',
    features: [
      { label: 'Up to 5 users', included: true },
      { label: '5 custom objects', included: true },
      { label: '10 workflows', included: true },
      { label: '3 dashboards', included: true },
      { label: '1,000 records per object', included: true },
      { label: 'AI Config (plain English setup)', included: true },
      { label: 'AI Insights & Predictions', included: false },
      { label: 'Email open/click tracking', included: true },
      { label: 'REST API access', included: false },
      { label: 'Integrations (Zapier, Slack)', included: false },
    ],
  },
  {
    name: 'PROFESSIONAL',
    price: '$59',
    priceDetail: 'per user / month',
    icon: <PremiumIcon />,
    color: '#7b1fa2',
    popular: true,
    features: [
      { label: 'Up to 25 users', included: true },
      { label: '50 custom objects', included: true },
      { label: '100 workflows', included: true },
      { label: '20 dashboards', included: true },
      { label: '50,000 records per object', included: true },
      { label: 'AI Config (plain English setup)', included: true },
      { label: 'AI Insights & Predictions', included: true },
      { label: 'Email open/click tracking', included: true },
      { label: 'REST API access', included: true },
      { label: 'Integrations (Zapier, Slack)', included: true },
    ],
  },
  {
    name: 'ENTERPRISE',
    price: '$99',
    priceDetail: 'per user / month',
    icon: <DiamondIcon />,
    color: '#f57c00',
    features: [
      { label: 'Unlimited users', included: true },
      { label: 'Unlimited custom objects', included: true },
      { label: 'Unlimited workflows', included: true },
      { label: 'Unlimited dashboards', included: true },
      { label: 'Unlimited records', included: true },
      { label: 'AI Config (plain English setup)', included: true },
      { label: 'AI Insights & Predictions', included: true },
      { label: 'Email open/click tracking', included: true },
      { label: 'REST API access', included: true },
      { label: 'Integrations (Zapier, Slack)', included: true },
    ],
  },
];

const PricingPage: React.FC = () => {
  const [currentPlan, setCurrentPlan] = useState<TenantPlan | null>(null);
  const [loading, setLoading] = useState(true);
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false,
    message: '',
    severity: 'success',
  });

  useEffect(() => {
    fetchPlan();
  }, []);

  const fetchPlan = async () => {
    try {
      const res = await api.get<ApiResponse<TenantPlan>>('/api/v1/auth/plan');
      setCurrentPlan(res.data.data);
    } catch {
      // If endpoint not available, default to FREE
      setCurrentPlan({ planName: 'FREE' } as TenantPlan);
    } finally {
      setLoading(false);
    }
  };

  const handleUpgrade = async (planName: string) => {
    try {
      const res = await api.put<ApiResponse<TenantPlan>>(`/api/v1/auth/plan/upgrade?plan=${planName}`);
      setCurrentPlan(res.data.data);
      setSnackbar({ open: true, message: `Successfully upgraded to ${planName}!`, severity: 'success' });
    } catch {
      setSnackbar({ open: true, message: 'Upgrade failed. Contact support.', severity: 'error' });
    }
  };

  const planOrder = ['FREE', 'STARTER', 'PROFESSIONAL', 'ENTERPRISE'];
  const currentIndex = planOrder.indexOf(currentPlan?.planName ?? 'FREE');

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" fontWeight={700} gutterBottom>
        Pricing & Plans
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 1 }}>
        Choose the plan that fits your business. Upgrade or downgrade anytime.
      </Typography>

      {currentPlan && (
        <Alert severity="info" sx={{ mb: 3 }}>
          Your current plan: <strong>{currentPlan.planName}</strong>
          {currentPlan.planName === 'FREE' && ' — Upgrade to unlock more features!'}
        </Alert>
      )}

      <Grid container spacing={3}>
        {PLANS.map((plan, idx) => {
          const isCurrent = plan.name === currentPlan?.planName;
          const isLower = idx < currentIndex;

          return (
            <Grid item xs={12} sm={6} md={3} key={plan.name}>
              <Card
                elevation={plan.popular ? 8 : 2}
                sx={{
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  border: isCurrent ? `2px solid ${plan.color}` : plan.popular ? `2px solid ${plan.color}` : '1px solid #e0e0e0',
                  position: 'relative',
                  transition: 'transform 0.2s',
                  '&:hover': { transform: 'translateY(-4px)' },
                }}
              >
                {plan.popular && (
                  <Chip
                    label="MOST POPULAR"
                    size="small"
                    sx={{
                      position: 'absolute',
                      top: -12,
                      left: '50%',
                      transform: 'translateX(-50%)',
                      bgcolor: plan.color,
                      color: '#fff',
                      fontWeight: 700,
                      fontSize: '0.7rem',
                    }}
                  />
                )}
                {isCurrent && (
                  <Chip
                    label="CURRENT PLAN"
                    size="small"
                    sx={{
                      position: 'absolute',
                      top: -12,
                      right: 12,
                      bgcolor: '#4caf50',
                      color: '#fff',
                      fontWeight: 700,
                      fontSize: '0.7rem',
                    }}
                  />
                )}

                <CardContent sx={{ flexGrow: 1, pt: 3 }}>
                  <Box sx={{ textAlign: 'center', mb: 2 }}>
                    <Box sx={{ color: plan.color, mb: 1 }}>{plan.icon}</Box>
                    <Typography variant="h6" fontWeight={700}>
                      {plan.name}
                    </Typography>
                    <Typography variant="h3" fontWeight={800} sx={{ color: plan.color, my: 1 }}>
                      {plan.price}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {plan.priceDetail}
                    </Typography>
                  </Box>

                  <Divider sx={{ my: 2 }} />

                  <List dense disablePadding>
                    {plan.features.map((feat) => (
                      <ListItem key={feat.label} disableGutters sx={{ py: 0.3 }}>
                        <ListItemIcon sx={{ minWidth: 28 }}>
                          {feat.included ? (
                            <CheckIcon fontSize="small" sx={{ color: '#4caf50' }} />
                          ) : (
                            <CloseIcon fontSize="small" sx={{ color: '#ccc' }} />
                          )}
                        </ListItemIcon>
                        <ListItemText
                          primary={feat.label}
                          primaryTypographyProps={{
                            variant: 'body2',
                            color: feat.included ? 'text.primary' : 'text.disabled',
                          }}
                        />
                      </ListItem>
                    ))}
                  </List>
                </CardContent>

                <Box sx={{ p: 2, pt: 0 }}>
                  {isCurrent ? (
                    <Button fullWidth variant="outlined" disabled>
                      Current Plan
                    </Button>
                  ) : isLower ? (
                    <Button fullWidth variant="outlined" color="inherit" disabled>
                      Downgrade
                    </Button>
                  ) : (
                    <Button
                      fullWidth
                      variant="contained"
                      sx={{ bgcolor: plan.color, '&:hover': { bgcolor: plan.color, opacity: 0.9 } }}
                      onClick={() => handleUpgrade(plan.name)}
                    >
                      {plan.name === 'FREE' ? 'Get Started' : `Upgrade to ${plan.name}`}
                    </Button>
                  )}
                </Box>
              </Card>
            </Grid>
          );
        })}
      </Grid>

      <Box sx={{ mt: 4, p: 3, bgcolor: '#f5f5f5', borderRadius: 2 }}>
        <Typography variant="h6" fontWeight={600} gutterBottom>
          Frequently Asked Questions
        </Typography>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Typography variant="subtitle2" fontWeight={600}>Can I upgrade anytime?</Typography>
            <Typography variant="body2" color="text.secondary">
              Yes! Upgrades take effect immediately. Your new limits apply right away.
            </Typography>
          </Grid>
          <Grid item xs={12} md={6}>
            <Typography variant="subtitle2" fontWeight={600}>What happens when I hit a limit?</Typography>
            <Typography variant="body2" color="text.secondary">
              You'll see a friendly message suggesting an upgrade. Existing data is never deleted.
            </Typography>
          </Grid>
          <Grid item xs={12} md={6}>
            <Typography variant="subtitle2" fontWeight={600}>Is there a free trial for paid plans?</Typography>
            <Typography variant="body2" color="text.secondary">
              Yes — all paid plans include a 14-day free trial. No credit card required.
            </Typography>
          </Grid>
          <Grid item xs={12} md={6}>
            <Typography variant="subtitle2" fontWeight={600}>Can I cancel anytime?</Typography>
            <Typography variant="body2" color="text.secondary">
              Absolutely. Cancel anytime and you'll revert to the Free plan. No data is lost.
            </Typography>
          </Grid>
        </Grid>
      </Box>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
      >
        <Alert severity={snackbar.severity} onClose={() => setSnackbar((s) => ({ ...s, open: false }))}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default PricingPage;
