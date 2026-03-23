/* ============================================================
   Auth Slice – JWT token + user state management
   ============================================================ */
import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { authService } from '../../services';
import type { UserProfile, LoginRequest, RegisterRequest } from '../../types';

interface AuthState {
  user: UserProfile | null;
  token: string | null;
  loading: boolean;
  error: string | null;
  isAuthenticated: boolean;
  mfaRequired: boolean;
  mfaPending: { userId: string; email: string; tenantId: string; mfaToken: string } | null;
}

const initialState: AuthState = {
  user: null,
  token: localStorage.getItem('accessToken'),
  loading: false,
  error: null,
  isAuthenticated: !!localStorage.getItem('accessToken'),
  mfaRequired: false,
  mfaPending: null,
};

export const login = createAsyncThunk('auth/login', async (data: LoginRequest, { rejectWithValue }) => {
  try {
    const res = await authService.login(data);
    if (res.data.mfaRequired) {
      return { mfaRequired: true, userId: res.data.userId, email: res.data.email, tenantId: res.data.tenantId, mfaToken: res.data.mfaToken };
    }
    localStorage.setItem('accessToken', res.data.accessToken);
    localStorage.setItem('refreshToken', res.data.refreshToken);
    return res.data;
  } catch (err: any) {
    return rejectWithValue(err.response?.data?.message || 'Login failed');
  }
});

export const verifyMfa = createAsyncThunk('auth/verifyMfa', async (data: { userId: string; code: string; tenantId: string; mfaToken: string }, { rejectWithValue }) => {
  try {
    const res = await authService.verifyMfa(data);
    localStorage.setItem('accessToken', res.data.accessToken);
    localStorage.setItem('refreshToken', res.data.refreshToken);
    return res.data;
  } catch (err: any) {
    return rejectWithValue(err.response?.data?.message || 'MFA verification failed');
  }
});

export const register = createAsyncThunk('auth/register', async (data: RegisterRequest, { rejectWithValue }) => {
  try {
    const res = await authService.register(data);
    localStorage.setItem('accessToken', res.data.accessToken);
    localStorage.setItem('refreshToken', res.data.refreshToken);
    return res.data;
  } catch (err: any) {
    return rejectWithValue(err.response?.data?.message || 'Registration failed');
  }
});

export const fetchProfile = createAsyncThunk('auth/fetchProfile', async (_, { rejectWithValue }) => {
  try {
    const res = await authService.getProfile();
    return res.data;
  } catch (err: any) {
    return rejectWithValue(err.response?.data?.message || 'Failed to fetch profile');
  }
});

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout(state) {
      state.user = null;
      state.token = null;
      state.isAuthenticated = false;
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('planName');
    },
    clearError(state) {
      state.error = null;
    },
    clearMfa(state) {
      state.mfaRequired = false;
      state.mfaPending = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // Login
      .addCase(login.pending, (state) => { state.loading = true; state.error = null; })
      .addCase(login.fulfilled, (state, action: PayloadAction<any>) => {
        state.loading = false;
        if (action.payload.mfaRequired) {
          state.mfaRequired = true;
          state.mfaPending = {
            userId: action.payload.userId,
            email: action.payload.email,
            tenantId: action.payload.tenantId,
            mfaToken: action.payload.mfaToken,
          };
        } else {
          state.token = action.payload.accessToken;
          state.isAuthenticated = true;
          state.mfaRequired = false;
          state.mfaPending = null;
          if (action.payload.planName) {
            localStorage.setItem('planName', action.payload.planName);
          }
        }
      })
      .addCase(login.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      // Verify MFA
      .addCase(verifyMfa.pending, (state) => { state.loading = true; state.error = null; })
      .addCase(verifyMfa.fulfilled, (state, action: PayloadAction<any>) => {
        state.loading = false;
        state.token = action.payload.accessToken;
        state.isAuthenticated = true;
        state.mfaRequired = false;
        state.mfaPending = null;
        if (action.payload.planName) {
          localStorage.setItem('planName', action.payload.planName);
        }
      })
      .addCase(verifyMfa.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      // Register
      .addCase(register.pending, (state) => { state.loading = true; state.error = null; })
      .addCase(register.fulfilled, (state, action: PayloadAction<any>) => {
        state.loading = false;
        state.token = action.payload.accessToken;
        state.isAuthenticated = true;
        if (action.payload.planName) {
          localStorage.setItem('planName', action.payload.planName);
        }
      })
      .addCase(register.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      // Profile
      .addCase(fetchProfile.fulfilled, (state, action: PayloadAction<UserProfile>) => {
        state.user = action.payload;
      });
  },
});

export const { logout, clearError, clearMfa } = authSlice.actions;
export default authSlice.reducer;
