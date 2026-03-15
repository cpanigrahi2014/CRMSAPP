/* ============================================================
   Sidebar – Left navigation menu (enterprise CRM style)
   ============================================================ */
import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  Box,
  Divider,
  useTheme,
} from '@mui/material';
import {
  Dashboard as DashboardIcon,
  People as LeadsIcon,
  Business as AccountsIcon,
  ContactPhone as ContactsIcon,
  TrendingUp as OpportunitiesIcon,
  SupportAgent as CasesIcon,
  TaskAlt as ActivitiesIcon,
  Campaign as CampaignsIcon,
  Assessment as ReportsIcon,
  Settings as SettingsIcon,
  Hub as LogoIcon,
  AutoFixHigh as AiConfigIcon,
  Email as EmailIcon,
  AccountTree as WorkflowIcon,
  Security as SecurityIcon,
  Extension as IntegrationIcon,
  Psychology as AiInsightsIcon,
  RocketLaunch as ZeroConfigIcon,
  Forum as CommunicationsIcon,
  Groups as CollaborationIcon,
  SmartToy as AutomationIcon,
  Code as DeveloperIcon,
  DataObject as ObjectManagerIcon,
  Payments as PricingIcon,
  Screenshot as ScreenCaptureIcon,
} from '@mui/icons-material';

export const DRAWER_WIDTH = 260;

interface SidebarProps {
  open: boolean;
  onClose?: () => void;
  variant?: 'permanent' | 'temporary';
}

const menuItems = [
  { label: 'Dashboard', path: '/dashboard', icon: <DashboardIcon /> },
  { label: 'Leads', path: '/leads', icon: <LeadsIcon /> },
  { label: 'Accounts', path: '/accounts', icon: <AccountsIcon /> },
  { label: 'Contacts', path: '/contacts', icon: <ContactsIcon /> },
  { label: 'Opportunities', path: '/opportunities', icon: <OpportunitiesIcon /> },
  { label: 'Activities', path: '/activities', icon: <ActivitiesIcon /> },
  { label: 'Email', path: '/email', icon: <EmailIcon /> },
  { label: 'Cases', path: '/cases', icon: <CasesIcon /> },
  { label: 'Campaigns', path: '/campaigns', icon: <CampaignsIcon /> },
  { label: 'Reports', path: '/reports', icon: <ReportsIcon /> },
  { label: 'Workflows', path: '/workflows', icon: <WorkflowIcon /> },    { label: 'Security', path: '/security', icon: <SecurityIcon /> },
  { label: 'Integrations', path: '/integrations', icon: <IntegrationIcon /> },
  { label: 'AI Config', path: '/ai-config', icon: <AiConfigIcon /> },
  { label: 'AI Insights', path: '/ai-insights', icon: <AiInsightsIcon /> },
  { label: 'Zero Config', path: '/zero-config', icon: <ZeroConfigIcon /> },
  { label: 'Communications', path: '/communications', icon: <CommunicationsIcon /> },
  { label: 'Collaboration', path: '/collaboration', icon: <CollaborationIcon /> },
  { label: 'Automation', path: '/automation', icon: <AutomationIcon /> },
  { label: 'Developer', path: '/developer', icon: <DeveloperIcon /> },
  { label: 'Object Manager', path: '/object-manager', icon: <ObjectManagerIcon /> },
  { label: 'Pricing & Plans', path: '/pricing', icon: <PricingIcon /> },
  { label: 'Screen Capture', path: '/screen-capture', icon: <ScreenCaptureIcon /> },
  { label: 'Settings', path: '/settings', icon: <SettingsIcon /> },
];

const Sidebar: React.FC<SidebarProps> = ({ open, onClose, variant = 'permanent' }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const theme = useTheme();

  const isActive = (path: string) => {
    if (path === '/dashboard') return location.pathname === '/dashboard' || location.pathname === '/';
    return location.pathname.startsWith(path);
  };

  return (
    <Drawer
      variant={variant}
      open={open}
      onClose={onClose}
      sx={{
        width: DRAWER_WIDTH,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width: DRAWER_WIDTH,
          boxSizing: 'border-box',
          borderRight: `1px solid ${theme.palette.divider}`,
          background: theme.palette.background.paper,
        },
      }}
    >
      {/* Logo / Brand */}
      <Toolbar sx={{ px: 2, py: 1 }}>
        <LogoIcon sx={{ color: 'primary.main', fontSize: 32, mr: 1.5 }} />
        <Box>
          <Typography variant="h6" noWrap sx={{ lineHeight: 1.2, color: 'primary.main' }}>
            CRM Platform
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Enterprise Sales Suite
          </Typography>
        </Box>
      </Toolbar>
      <Divider />

      {/* Navigation */}
      <List sx={{ px: 1, pt: 1 }}>
        {menuItems.map((item) => {
          const active = isActive(item.path);
          return (
            <ListItem key={item.path} disablePadding sx={{ mb: 0.5 }}>
              <ListItemButton
                onClick={() => { navigate(item.path); onClose?.(); }}
                sx={{
                  borderRadius: 2,
                  mx: 0.5,
                  ...(active && {
                    backgroundColor: 'primary.main',
                    color: '#fff',
                    '&:hover': { backgroundColor: 'primary.dark' },
                    '& .MuiListItemIcon-root': { color: '#fff' },
                  }),
                }}
              >
                <ListItemIcon sx={{ minWidth: 40, color: active ? '#fff' : 'text.secondary' }}>
                  {item.icon}
                </ListItemIcon>
                <ListItemText
                  primary={item.label}
                  primaryTypographyProps={{ fontSize: 14, fontWeight: active ? 600 : 400 }}
                />
              </ListItemButton>
            </ListItem>
          );
        })}
      </List>
    </Drawer>
  );
};

export default Sidebar;
