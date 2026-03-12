/* ============================================================
   DraggableWidgetGrid – wraps MUI Grid items in a @dnd-kit
   sortable context so dashboard widgets can be rearranged.
   Layout persists in localStorage.
   ============================================================ */
import React, { useState, useCallback, useEffect } from 'react';
import {
  DndContext,
  DragEndEvent,
  DragOverlay,
  DragStartEvent,
  PointerSensor,
  KeyboardSensor,
  useSensor,
  useSensors,
  closestCenter,
} from '@dnd-kit/core';
import {
  SortableContext,
  sortableKeyboardCoordinates,
  rectSortingStrategy,
  useSortable,
  arrayMove,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import {
  Box,
  IconButton,
  Tooltip,
  Typography,
  Chip,
} from '@mui/material';
import { DragIndicator, RestartAlt } from '@mui/icons-material';

const STORAGE_KEY = 'crm-dashboard-layout';

export interface DashboardWidget {
  id: string;
  content: React.ReactNode;
}

interface SortableWidgetProps {
  id: string;
  children: React.ReactNode;
}

const SortableWidget: React.FC<SortableWidgetProps> = ({ id, children }) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id });

  const style: React.CSSProperties = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.4 : 1,
    position: 'relative',
  };

  return (
    <Box ref={setNodeRef} style={style} sx={{ position: 'relative' }}>
      {/* Drag handle */}
      <Box
        {...attributes}
        {...listeners}
        sx={{
          position: 'absolute',
          top: 4,
          right: 4,
          zIndex: 10,
          cursor: 'grab',
          '&:active': { cursor: 'grabbing' },
          opacity: 0.4,
          '&:hover': { opacity: 1 },
          bgcolor: 'background.paper',
          borderRadius: 1,
          p: 0.25,
        }}
      >
        <DragIndicator fontSize="small" />
      </Box>
      {children}
    </Box>
  );
};

interface DraggableWidgetGridProps {
  widgets: DashboardWidget[];
  storageKey?: string;
}

const DraggableWidgetGrid: React.FC<DraggableWidgetGridProps> = ({
  widgets,
  storageKey = STORAGE_KEY,
}) => {
  const [orderedIds, setOrderedIds] = useState<string[]>(() => {
    try {
      const saved = localStorage.getItem(storageKey);
      if (saved) {
        const parsed = JSON.parse(saved) as string[];
        // Validate saved order against current widgets
        const widgetIds = new Set(widgets.map((w) => w.id));
        const valid = parsed.filter((id) => widgetIds.has(id));
        // Append any new widgets not in saved order
        const remaining = widgets.filter((w) => !parsed.includes(w.id)).map((w) => w.id);
        return [...valid, ...remaining];
      }
    } catch { /* ignore */ }
    return widgets.map((w) => w.id);
  });

  const [activeId, setActiveId] = useState<string | null>(null);

  // Sync when widgets change
  useEffect(() => {
    const widgetIds = new Set(widgets.map((w) => w.id));
    setOrderedIds((prev) => {
      const valid = prev.filter((id) => widgetIds.has(id));
      const remaining = widgets.filter((w) => !prev.includes(w.id)).map((w) => w.id);
      return [...valid, ...remaining];
    });
  }, [widgets]);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );

  const handleDragStart = useCallback((event: DragStartEvent) => {
    setActiveId(String(event.active.id));
  }, []);

  const handleDragEnd = useCallback(
    (event: DragEndEvent) => {
      setActiveId(null);
      const { active, over } = event;
      if (!over || active.id === over.id) return;

      setOrderedIds((prev) => {
        const oldIndex = prev.indexOf(String(active.id));
        const newIndex = prev.indexOf(String(over.id));
        const newOrder = arrayMove(prev, oldIndex, newIndex);
        localStorage.setItem(storageKey, JSON.stringify(newOrder));
        return newOrder;
      });
    },
    [storageKey],
  );

  const handleReset = () => {
    const defaultOrder = widgets.map((w) => w.id);
    setOrderedIds(defaultOrder);
    localStorage.removeItem(storageKey);
  };

  const widgetMap = new Map(widgets.map((w) => [w.id, w]));
  const orderedWidgets = orderedIds.map((id) => widgetMap.get(id)).filter(Boolean) as DashboardWidget[];

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', mb: 1, gap: 1 }}>
        <Chip
          icon={<DragIndicator />}
          label="Drag widgets to rearrange"
          size="small"
          variant="outlined"
          color="info"
        />
        <Tooltip title="Reset layout to default">
          <IconButton size="small" onClick={handleReset}>
            <RestartAlt fontSize="small" />
          </IconButton>
        </Tooltip>
      </Box>
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
      >
        <SortableContext items={orderedIds} strategy={rectSortingStrategy}>
          {orderedWidgets.map((widget) => (
            <SortableWidget key={widget.id} id={widget.id}>
              {widget.content}
            </SortableWidget>
          ))}
        </SortableContext>
        <DragOverlay>
          {activeId && widgetMap.get(activeId) && (
            <Box sx={{ opacity: 0.85, boxShadow: 6 }}>
              {widgetMap.get(activeId)!.content}
            </Box>
          )}
        </DragOverlay>
      </DndContext>
    </Box>
  );
};

export default DraggableWidgetGrid;
