/* ============================================================
   ExportMenu – dropdown to export report data as CSV or PDF
   ============================================================ */
import React, { useState } from 'react';
import {
  Button,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
} from '@mui/material';
import {
  FileDownload,
  TableChart,
  PictureAsPdf,
} from '@mui/icons-material';

interface ExportableData {
  headers: string[];
  rows: (string | number)[][];
  title: string;
}

interface ExportMenuProps {
  getData: () => ExportableData[];
}

function downloadCSV(data: ExportableData) {
  const csvRows: string[] = [];
  csvRows.push(data.headers.map((h) => `"${h}"`).join(','));
  for (const row of data.rows) {
    csvRows.push(row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','));
  }
  const csvContent = csvRows.join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${data.title.replace(/\s+/g, '_')}_${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

function downloadAllCSV(datasets: ExportableData[]) {
  for (const data of datasets) {
    downloadCSV(data);
  }
}

function printAsPDF(datasets: ExportableData[]) {
  const printWindow = window.open('', '_blank');
  if (!printWindow) return;

  let html = `<!DOCTYPE html><html><head><title>CRM Report</title>
    <style>
      body { font-family: Arial, sans-serif; padding: 20px; }
      h1 { color: #1976d2; margin-bottom: 4px; }
      h2 { color: #333; margin-top: 24px; }
      table { border-collapse: collapse; width: 100%; margin-bottom: 24px; }
      th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
      th { background-color: #f5f5f5; font-weight: 600; }
      tr:nth-child(even) { background-color: #fafafa; }
      .timestamp { color: #666; font-size: 12px; }
    </style>
  </head><body>`;
  html += `<h1>CRM Analytics Report</h1>`;
  html += `<p class="timestamp">Generated: ${new Date().toLocaleString()}</p>`;

  for (const data of datasets) {
    html += `<h2>${data.title}</h2>`;
    html += '<table><thead><tr>';
    for (const h of data.headers) {
      html += `<th>${h}</th>`;
    }
    html += '</tr></thead><tbody>';
    for (const row of data.rows) {
      html += '<tr>';
      for (const cell of row) {
        html += `<td>${cell}</td>`;
      }
      html += '</tr>';
    }
    html += '</tbody></table>';
  }

  html += '</body></html>';
  printWindow.document.write(html);
  printWindow.document.close();
  printWindow.print();
}

const ExportMenu: React.FC<ExportMenuProps> = ({ getData }) => {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const handleCSV = () => {
    setAnchorEl(null);
    const data = getData();
    downloadAllCSV(data);
  };

  const handlePDF = () => {
    setAnchorEl(null);
    const data = getData();
    printAsPDF(data);
  };

  return (
    <>
      <Button
        variant="outlined"
        size="small"
        startIcon={<FileDownload />}
        onClick={(e) => setAnchorEl(e.currentTarget)}
      >
        Export
      </Button>
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
      >
        <MenuItem onClick={handleCSV}>
          <ListItemIcon><TableChart fontSize="small" /></ListItemIcon>
          <ListItemText>Export as CSV</ListItemText>
        </MenuItem>
        <MenuItem onClick={handlePDF}>
          <ListItemIcon><PictureAsPdf fontSize="small" /></ListItemIcon>
          <ListItemText>Export as PDF</ListItemText>
        </MenuItem>
      </Menu>
    </>
  );
};

export default ExportMenu;
export type { ExportableData };
