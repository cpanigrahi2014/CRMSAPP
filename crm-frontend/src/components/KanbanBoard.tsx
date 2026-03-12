/* ============================================================
   KanbanBoard – drag-and-drop columns using @dnd-kit
   ============================================================ */
import React, { useState } from 'react';
import {
  DndContext,
  DragEndEvent,
  DragOverlay,
  DragStartEvent,
  PointerSensor,
  useSensor,
  useSensors,
  closestCorners,
} from '@dnd-kit/core';
import {
  SortableContext,
  verticalListSortingStrategy,
  useSortable,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import {
  Box,
  Card,
  CardContent,
  Chip,
  Paper,
  Typography,
  useTheme,
  Avatar,
} from '@mui/material';

/* ---- Types ---- */
export interface KanbanItem {
  id: string;
  title: string;
  subtitle?: string;
  value?: string;
  tag?: string;
  tagColor?: 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
  avatarText?: string;
}

export interface KanbanColumn {
  id: string;
  title: string;
  color?: string;
  items: KanbanItem[];
}

interface KanbanBoardProps {
  columns: KanbanColumn[];
  onDragEnd: (itemId: string, fromColumn: string, toColumn: string) => void;
  onItemClick?: (itemId: string) => void;
}

/* ---- Sortable Card ---- */
const SortableCard: React.FC<{ item: KanbanItem; onItemClick?: (id: string) => void }> = ({ item, onItemClick }) => {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: item.id,
  });
  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.4 : 1,
  };

  return (
    <Card
      ref={setNodeRef}
      {...attributes}
      {...listeners}
      onClick={() => onItemClick?.(item.id)}
      sx={{ mb: 1.5, cursor: 'grab', '&:active': { cursor: 'grabbing' } }}
      style={style}
    >
      <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Typography variant="subtitle2" fontWeight={600} gutterBottom noWrap>
          {item.title}
        </Typography>
        {item.subtitle && (
          <Typography variant="caption" color="text.secondary" display="block" noWrap>
            {item.subtitle}
          </Typography>
        )}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 1 }}>
          {item.value && (
            <Typography variant="body2" fontWeight={600} color="primary">
              {item.value}
            </Typography>
          )}
          {item.tag && <Chip label={item.tag} size="small" color={item.tagColor ?? 'default'} />}
          {item.avatarText && (
            <Avatar sx={{ width: 24, height: 24, fontSize: 11, bgcolor: 'primary.main' }}>
              {item.avatarText}
            </Avatar>
          )}
        </Box>
      </CardContent>
    </Card>
  );
};

/* ---- Column ---- */
const Column: React.FC<{ column: KanbanColumn; onItemClick?: (id: string) => void }> = ({ column, onItemClick }) => {
  const theme = useTheme();
  return (
    <Paper
      variant="outlined"
      sx={{
        minWidth: 280,
        maxWidth: 320,
        flex: '1 1 280px',
        backgroundColor: theme.palette.mode === 'dark' ? 'grey.900' : 'grey.50',
        borderRadius: 2,
        display: 'flex',
        flexDirection: 'column',
        maxHeight: '75vh',
      }}
    >
      {/* Column header */}
      <Box
        sx={{
          p: 1.5,
          display: 'flex',
          alignItems: 'center',
          gap: 1,
          borderBottom: `2px solid ${column.color || theme.palette.primary.main}`,
        }}
      >
        <Typography variant="subtitle2" fontWeight={700}>
          {column.title}
        </Typography>
        <Chip label={column.items.length} size="small" />
      </Box>

      {/* Cards */}
      <Box sx={{ p: 1.5, overflowY: 'auto', flex: 1 }}>
        <SortableContext items={column.items.map((i) => i.id)} strategy={verticalListSortingStrategy}>
          {column.items.map((item) => (
            <SortableCard key={item.id} item={item} onItemClick={onItemClick} />
          ))}
        </SortableContext>
      </Box>
    </Paper>
  );
};

/* ---- Board ---- */
const KanbanBoard: React.FC<KanbanBoardProps> = ({ columns, onDragEnd, onItemClick }) => {
  const [activeId, setActiveId] = useState<string | null>(null);
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }));

  const findColumn = (itemId: string) => columns.find((c) => c.items.some((i) => i.id === itemId));

  const handleDragStart = (event: DragStartEvent) => setActiveId(String(event.active.id));

  const handleDragEnd = (event: DragEndEvent) => {
    setActiveId(null);
    const { active, over } = event;
    if (!over) return;

    const fromCol = findColumn(String(active.id));
    // over might be a column id or another item id
    let toCol = columns.find((c) => c.id === String(over.id)) ?? findColumn(String(over.id));
    if (!fromCol || !toCol) return;
    if (fromCol.id === toCol.id) return;

    onDragEnd(String(active.id), fromCol.id, toCol.id);
  };

  const activeItem = activeId
    ? columns.flatMap((c) => c.items).find((i) => i.id === activeId)
    : null;

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCorners}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
    >
      <Box
        sx={{
          display: 'flex',
          gap: 2,
          overflowX: 'auto',
          pb: 2,
        }}
      >
        {columns.map((col) => (
          <Column key={col.id} column={col} onItemClick={onItemClick} />
        ))}
      </Box>

      <DragOverlay>
        {activeItem && (
          <Card sx={{ width: 280, opacity: 0.85, boxShadow: 6 }}>
            <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Typography variant="subtitle2" fontWeight={600}>
                {activeItem.title}
              </Typography>
            </CardContent>
          </Card>
        )}
      </DragOverlay>
    </DndContext>
  );
};

export default KanbanBoard;
