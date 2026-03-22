/* ============================================================
   DataTable – wrapper around MUI X DataGrid
   ============================================================ */
import React from 'react';
import { DataGrid, GridColDef, GridRowsProp, GridPaginationModel, GridRowSelectionModel } from '@mui/x-data-grid';
import { Box, Card, TextField, InputAdornment, Button, Typography, Stack } from '@mui/material';
import { Search as SearchIcon, Add as AddIcon } from '@mui/icons-material';

interface Props {
  title: string;
  rows: GridRowsProp;
  columns: GridColDef[];
  loading?: boolean;
  searchValue?: string;
  onSearchChange?: (value: string) => void;
  onAdd?: () => void;
  addLabel?: string;
  paginationModel?: GridPaginationModel;
  onPaginationModelChange?: (model: GridPaginationModel) => void;
  rowCount?: number;
  paginationMode?: 'client' | 'server';
  toolbar?: React.ReactNode;
  checkboxSelection?: boolean;
  onRowSelectionModelChange?: (model: GridRowSelectionModel) => void;
  rowSelectionModel?: GridRowSelectionModel;
}

const DataTable: React.FC<Props> = ({
  title,
  rows,
  columns,
  loading = false,
  searchValue,
  onSearchChange,
  onAdd,
  addLabel = 'Add New',
  paginationModel,
  onPaginationModelChange,
  rowCount,
  paginationMode = 'client',
  toolbar,
  checkboxSelection,
  onRowSelectionModelChange,
  rowSelectionModel,
}) => (
  <Card sx={{ height: '100%' }}>
    {/* Header */}
    <Box
      sx={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: 2,
        p: 2,
        pb: 1,
      }}
    >
      <Typography variant="h6" fontWeight={600}>
        {title}
      </Typography>
      <Stack direction="row" spacing={1.5} alignItems="center">
        {onSearchChange !== undefined && (
          <TextField
            size="small"
            placeholder="Search…"
            value={searchValue ?? ''}
            onChange={(e) => onSearchChange(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" />
                </InputAdornment>
              ),
            }}
            sx={{ width: { xs: '100%', sm: 240 } }}
          />
        )}
        {toolbar}
        {onAdd && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={onAdd}>
            {addLabel}
          </Button>
        )}
      </Stack>
    </Box>

    {/* Grid */}
    <Box sx={{ width: '100%', p: 2, pt: 0 }}>
      <DataGrid
        rows={rows}
        columns={columns}
        loading={loading}
        autoHeight
        disableRowSelectionOnClick
        pageSizeOptions={[10, 25, 50]}
        paginationModel={paginationModel ?? { page: 0, pageSize: 10 }}
        onPaginationModelChange={onPaginationModelChange}
        rowCount={rowCount}
        paginationMode={paginationMode}
        checkboxSelection={checkboxSelection}
        onRowSelectionModelChange={onRowSelectionModelChange}
        rowSelectionModel={rowSelectionModel}
        sx={{
          border: 'none',
          '& .MuiDataGrid-columnHeaders': {
            backgroundColor: 'action.hover',
            borderRadius: 1,
          },
          '& .MuiDataGrid-cell': {
            borderBottom: '1px solid',
            borderColor: 'divider',
          },
        }}
      />
    </Box>
  </Card>
);

export default DataTable;
