/* ============================================================
   SettingsPage – user profile + app preferences
   ============================================================ */
import React, { useState } from 'react';
import {
  Grid,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Avatar,
  Box,
  Divider,
  Switch,
  FormControlLabel,
  MenuItem,
  Alert,
} from '@mui/material';
import { Save as SaveIcon } from '@mui/icons-material';
import { PageHeader } from '../components';
import { useAppSelector, useAppDispatch } from '../hooks/useRedux';
import { toggleTheme } from '../store/slices/uiSlice';
import { useSnackbar } from 'notistack';

const timezoneOptions = [
  'America/New_York',
  'America/Chicago',
  'America/Denver',
  'America/Los_Angeles',
  'Europe/London',
  'Europe/Berlin',
  'Asia/Tokyo',
  'Asia/Kolkata',
  'Australia/Sydney',
];

const SettingsPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const { user } = useAppSelector((s) => s.auth);
  const { themeMode } = useAppSelector((s) => s.ui);

  const [profile, setProfile] = useState({
    firstName: user?.firstName ?? '',
    lastName: user?.lastName ?? '',
    email: user?.email ?? '',
    phone: '',
    timezone: 'America/New_York',
    bio: '',
  });

  const [notifications, setNotifications] = useState({
    emailAlerts: true,
    pushNotifications: true,
    weeklyDigest: false,
    dealUpdates: true,
    taskReminders: true,
  });

  const handleProfileChange = (field: string) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setProfile((p) => ({ ...p, [field]: e.target.value }));

  const handleNotifToggle = (field: string) => () =>
    setNotifications((p) => ({ ...p, [field]: !(p as any)[field] }));

  const handleSave = () => {
    enqueueSnackbar('Settings saved successfully', { variant: 'success' });
  };

  return (
    <>
      <PageHeader
        title="Settings"
        breadcrumbs={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Settings' }]}
      />

      <Grid container spacing={3}>
        {/* Profile */}
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" fontWeight={600} gutterBottom>
                Profile Information
              </Typography>
              <Divider sx={{ mb: 3 }} />

              <Box sx={{ display: 'flex', alignItems: 'center', gap: 3, mb: 3 }}>
                <Avatar sx={{ width: 72, height: 72, fontSize: 28, bgcolor: 'primary.main' }}>
                  {(profile.firstName[0] ?? '') + (profile.lastName[0] ?? '')}
                </Avatar>
                <Box>
                  <Typography variant="h6">{profile.firstName} {profile.lastName}</Typography>
                  <Typography variant="body2" color="text.secondary">{profile.email}</Typography>
                  <Button size="small" sx={{ mt: 0.5 }}>Change Avatar</Button>
                </Box>
              </Box>

              <Grid container spacing={2}>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth label="First Name" value={profile.firstName} onChange={handleProfileChange('firstName')} />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth label="Last Name" value={profile.lastName} onChange={handleProfileChange('lastName')} />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth label="Email" type="email" value={profile.email} onChange={handleProfileChange('email')} />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth label="Phone" value={profile.phone} onChange={handleProfileChange('phone')} />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField fullWidth select label="Timezone" value={profile.timezone} onChange={handleProfileChange('timezone')}>
                    {timezoneOptions.map((tz) => <MenuItem key={tz} value={tz}>{tz}</MenuItem>)}
                  </TextField>
                </Grid>
                <Grid item xs={12}>
                  <TextField fullWidth multiline rows={3} label="Bio" value={profile.bio} onChange={handleProfileChange('bio')} />
                </Grid>
              </Grid>

              <Box sx={{ mt: 3, textAlign: 'right' }}>
                <Button variant="contained" startIcon={<SaveIcon />} onClick={handleSave}>
                  Save Changes
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Sidebar settings */}
        <Grid item xs={12} md={4}>
          {/* Appearance */}
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" fontWeight={600} gutterBottom>
                Appearance
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <FormControlLabel
                control={
                  <Switch
                    checked={themeMode === 'dark'}
                    onChange={() => dispatch(toggleTheme())}
                  />
                }
                label="Dark Mode"
              />
            </CardContent>
          </Card>

          {/* Notifications */}
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" fontWeight={600} gutterBottom>
                Notifications
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <FormControlLabel
                control={<Switch checked={notifications.emailAlerts} onChange={handleNotifToggle('emailAlerts')} />}
                label="Email Alerts"
              />
              <FormControlLabel
                control={<Switch checked={notifications.pushNotifications} onChange={handleNotifToggle('pushNotifications')} />}
                label="Push Notifications"
              />
              <FormControlLabel
                control={<Switch checked={notifications.weeklyDigest} onChange={handleNotifToggle('weeklyDigest')} />}
                label="Weekly Digest"
              />
              <FormControlLabel
                control={<Switch checked={notifications.dealUpdates} onChange={handleNotifToggle('dealUpdates')} />}
                label="Deal Updates"
              />
              <FormControlLabel
                control={<Switch checked={notifications.taskReminders} onChange={handleNotifToggle('taskReminders')} />}
                label="Task Reminders"
              />
            </CardContent>
          </Card>

          {/* Security */}
          <Card>
            <CardContent>
              <Typography variant="h6" fontWeight={600} gutterBottom>
                Security
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <Button variant="outlined" fullWidth sx={{ mb: 1.5 }}>
                Change Password
              </Button>
              <Button variant="outlined" fullWidth color="warning">
                Enable Two-Factor Auth
              </Button>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </>
  );
};

export default SettingsPage;
