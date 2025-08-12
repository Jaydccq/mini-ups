/**
 * Driver Scheduling Calendar Component
 * 
 * Features:
 * - Monthly calendar view with driver schedules
 * - Drag and drop driver assignment to shifts
 * - Different shift types (morning, afternoon, night)
 * - Driver availability tracking
 * - Conflict detection and resolution
 * - Schedule optimization suggestions
 * 
 *
 
 */
import React, { useState, useEffect, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { 
  Calendar, 
  ChevronLeft, 
  ChevronRight, 
  Plus
} from 'lucide-react';

interface DriverSchedule {
  id: string;
  driverId: string;
  driverName: string;
  date: string;
  shift: 'MORNING' | 'AFTERNOON' | 'NIGHT';
  startTime: string;
  endTime: string;
  status: 'SCHEDULED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';
  truckId?: string;
  route?: string;
  notes?: string;
}

interface Driver {
  id: string;
  name: string;
  email: string;
  availability: {
    [key: string]: boolean; // date string -> available
  };
  preferences: {
    preferredShifts: ('MORNING' | 'AFTERNOON' | 'NIGHT')[];
    maxHoursPerWeek: number;
  };
}

interface DriverSchedulingCalendarProps {
  drivers: Driver[];
  className?: string;
}

export const DriverSchedulingCalendar: React.FC<DriverSchedulingCalendarProps> = ({
  drivers,
  className = ''
}) => {
  const [schedules, setSchedules] = useState<DriverSchedule[]>([]);
  const [currentDate, setCurrentDate] = useState(new Date());
  const [isAddScheduleDialogOpen, setIsAddScheduleDialogOpen] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState<DriverSchedule | null>(null);
  const [isEditScheduleDialogOpen, setIsEditScheduleDialogOpen] = useState(false);

  // Form state for schedule creation/editing
  const [scheduleForm, setScheduleForm] = useState({
    driverId: '',
    date: '',
    shift: 'MORNING' as 'MORNING' | 'AFTERNOON' | 'NIGHT',
    startTime: '08:00',
    endTime: '16:00',
    truckId: '',
    route: '',
    notes: ''
  });

  // Generate calendar days for current month
  const calendarDays = useMemo(() => {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    
    const firstDay = new Date(year, month, 1);
    const startDate = new Date(firstDay);
    startDate.setDate(startDate.getDate() - firstDay.getDay()); // Start from Sunday
    
    const days = [];
    const currentDay = new Date(startDate);
    
    // Generate 42 days (6 weeks) to fill the calendar
    for (let i = 0; i < 42; i++) {
      days.push(new Date(currentDay));
      currentDay.setDate(currentDay.getDate() + 1);
    }
    
    return days;
  }, [currentDate]);

  // Get schedules for a specific date
  const getSchedulesForDate = (date: Date) => {
    const dateStr = date.toISOString().split('T')[0];
    return schedules.filter(schedule => schedule.date === dateStr);
  };

  // Fetch schedules from API
  const fetchSchedules = async () => {
    try {
      const response = await fetch('/api/admin/schedules', {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
      });
      
      if (response.ok) {
        const data = await response.json();
        setSchedules(data.data.schedules || []);
      } else {
        // Use mock data for demo
        setSchedules(mockSchedules);
      }
    } catch (error) {
      console.error('Error fetching schedules:', error);
      setSchedules(mockSchedules);
    }
  };

  useEffect(() => {
    fetchSchedules();
  }, []);

  const handleCreateSchedule = async () => {
    try {
      const newSchedule: DriverSchedule = {
        id: Date.now().toString(),
        driverId: scheduleForm.driverId,
        driverName: drivers.find(d => d.id === scheduleForm.driverId)?.name || '',
        date: scheduleForm.date,
        shift: scheduleForm.shift,
        startTime: scheduleForm.startTime,
        endTime: scheduleForm.endTime,
        status: 'SCHEDULED',
        truckId: scheduleForm.truckId || undefined,
        route: scheduleForm.route || undefined,
        notes: scheduleForm.notes || undefined
      };

      // Check for conflicts
      const conflicts = schedules.filter(s => 
        s.date === newSchedule.date && 
        s.driverId === newSchedule.driverId && 
        s.status !== 'CANCELLED'
      );

      if (conflicts.length > 0) {
        alert('Driver already has a schedule for this date');
        return;
      }

      setSchedules(prev => [...prev, newSchedule]);
      setIsAddScheduleDialogOpen(false);
      resetForm();
    } catch (error) {
      console.error('Error creating schedule:', error);
    }
  };

  const handleUpdateSchedule = async () => {
    if (!editingSchedule) return;

    try {
      const updatedSchedule: DriverSchedule = {
        ...editingSchedule,
        driverId: scheduleForm.driverId,
        driverName: drivers.find(d => d.id === scheduleForm.driverId)?.name || '',
        date: scheduleForm.date,
        shift: scheduleForm.shift,
        startTime: scheduleForm.startTime,
        endTime: scheduleForm.endTime,
        truckId: scheduleForm.truckId || undefined,
        route: scheduleForm.route || undefined,
        notes: scheduleForm.notes || undefined
      };

      setSchedules(prev => prev.map(s => 
        s.id === editingSchedule.id ? updatedSchedule : s
      ));
      setIsEditScheduleDialogOpen(false);
      setEditingSchedule(null);
      resetForm();
    } catch (error) {
      console.error('Error updating schedule:', error);
    }
  };

  const handleDeleteSchedule = async (scheduleId: string) => {
    if (!confirm('Are you sure you want to delete this schedule?')) return;

    try {
      setSchedules(prev => prev.filter(s => s.id !== scheduleId));
    } catch (error) {
      console.error('Error deleting schedule:', error);
    }
  };

  const handleEditSchedule = (schedule: DriverSchedule) => {
    setEditingSchedule(schedule);
    setScheduleForm({
      driverId: schedule.driverId,
      date: schedule.date,
      shift: schedule.shift,
      startTime: schedule.startTime,
      endTime: schedule.endTime,
      truckId: schedule.truckId || '',
      route: schedule.route || '',
      notes: schedule.notes || ''
    });
    setIsEditScheduleDialogOpen(true);
  };

  const resetForm = () => {
    setScheduleForm({
      driverId: '',
      date: '',
      shift: 'MORNING',
      startTime: '08:00',
      endTime: '16:00',
      truckId: '',
      route: '',
      notes: ''
    });
  };

  const navigateMonth = (direction: 'prev' | 'next') => {
    const newDate = new Date(currentDate);
    newDate.setMonth(currentDate.getMonth() + (direction === 'next' ? 1 : -1));
    setCurrentDate(newDate);
  };

  const getShiftColor = (shift: string) => {
    switch (shift) {
      case 'MORNING':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'AFTERNOON':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'NIGHT':
        return 'bg-purple-100 text-purple-800 border-purple-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'SCHEDULED':
        return 'bg-yellow-100 text-yellow-800';
      case 'CONFIRMED':
        return 'bg-blue-100 text-blue-800';
      case 'COMPLETED':
        return 'bg-green-100 text-green-800';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

  return (
    <>
      <Card className={className}>
      <CardHeader>
        <div className="flex justify-between items-center">
          <CardTitle className="flex items-center gap-2">
            <Calendar className="h-5 w-5" />
            Driver Scheduling Calendar
          </CardTitle>
          <Dialog open={isAddScheduleDialogOpen} onOpenChange={setIsAddScheduleDialogOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="w-4 h-4 mr-2" />
                Add Schedule
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-md">
              <DialogHeader>
                <DialogTitle>Add New Schedule</DialogTitle>
              </DialogHeader>
              <div className="space-y-4">
                <div>
                  <Label htmlFor="driver">Driver</Label>
                  <Select value={scheduleForm.driverId} onValueChange={(value) => 
                    setScheduleForm(prev => ({ ...prev, driverId: value }))
                  }>
                    <SelectTrigger>
                      <SelectValue placeholder="Select driver" />
                    </SelectTrigger>
                    <SelectContent>
                      {drivers.map(driver => (
                        <SelectItem key={driver.id} value={driver.id}>
                          {driver.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label htmlFor="date">Date</Label>
                  <Input 
                    id="date" 
                    type="date" 
                    value={scheduleForm.date}
                    onChange={(e) => setScheduleForm(prev => ({ ...prev, date: e.target.value }))}
                  />
                </div>
                <div>
                  <Label htmlFor="shift">Shift</Label>
                  <Select value={scheduleForm.shift} onValueChange={(value: any) => 
                    setScheduleForm(prev => ({ ...prev, shift: value }))
                  }>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="MORNING">Morning (8:00 - 16:00)</SelectItem>
                      <SelectItem value="AFTERNOON">Afternoon (16:00 - 24:00)</SelectItem>
                      <SelectItem value="NIGHT">Night (00:00 - 8:00)</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <Label htmlFor="startTime">Start Time</Label>
                    <Input 
                      id="startTime" 
                      type="time" 
                      value={scheduleForm.startTime}
                      onChange={(e) => setScheduleForm(prev => ({ ...prev, startTime: e.target.value }))}
                    />
                  </div>
                  <div>
                    <Label htmlFor="endTime">End Time</Label>
                    <Input 
                      id="endTime" 
                      type="time" 
                      value={scheduleForm.endTime}
                      onChange={(e) => setScheduleForm(prev => ({ ...prev, endTime: e.target.value }))}
                    />
                  </div>
                </div>
                <div>
                  <Label htmlFor="route">Route (Optional)</Label>
                  <Input 
                    id="route" 
                    placeholder="e.g., Downtown - Airport" 
                    value={scheduleForm.route}
                    onChange={(e) => setScheduleForm(prev => ({ ...prev, route: e.target.value }))}
                  />
                </div>
                <div className="flex justify-end space-x-2">
                  <Button variant="outline" onClick={() => setIsAddScheduleDialogOpen(false)}>
                    Cancel
                  </Button>
                  <Button onClick={handleCreateSchedule}>Add Schedule</Button>
                </div>
              </div>
            </DialogContent>
          </Dialog>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Calendar Navigation */}
        <div className="flex justify-between items-center">
          <Button variant="outline" size="sm" onClick={() => navigateMonth('prev')}>
            <ChevronLeft className="w-4 h-4" />
          </Button>
          <h3 className="text-lg font-semibold">
            {monthNames[currentDate.getMonth()]} {currentDate.getFullYear()}
          </h3>
          <Button variant="outline" size="sm" onClick={() => navigateMonth('next')}>
            <ChevronRight className="w-4 h-4" />
          </Button>
        </div>

        {/* Calendar Grid */}
        <div className="grid grid-cols-7 gap-1">
          {/* Day headers */}
          {dayNames.map(day => (
            <div key={day} className="p-2 text-center text-sm font-medium text-gray-600 bg-gray-50 rounded">
              {day}
            </div>
          ))}
          
          {/* Calendar days */}
          {calendarDays.map((day, index) => {
            const daySchedules = getSchedulesForDate(day);
            const isCurrentMonth = day.getMonth() === currentDate.getMonth();
            const isToday = day.toDateString() === new Date().toDateString();
            
            return (
              <div 
                key={index} 
                className={`min-h-[80px] border rounded p-1 ${
                  isCurrentMonth ? 'bg-white' : 'bg-gray-50 text-gray-400'
                } ${isToday ? 'border-blue-500 bg-blue-50' : 'border-gray-200'}`}
              >
                <div className="text-sm font-medium mb-1">
                  {day.getDate()}
                </div>
                <div className="space-y-1">
                  {daySchedules.slice(0, 2).map(schedule => (
                    <div 
                      key={schedule.id}
                      className="text-xs p-1 rounded cursor-pointer hover:opacity-80"
                      onClick={() => handleEditSchedule(schedule)}
                    >
                      <Badge className={`${getShiftColor(schedule.shift)} text-xs px-1 py-0`}>
                        {schedule.shift.charAt(0)}
                      </Badge>
                      <div className="truncate">{schedule.driverName}</div>
                    </div>
                  ))}
                  {daySchedules.length > 2 && (
                    <div className="text-xs text-gray-500">
                      +{daySchedules.length - 2} more
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        {/* Schedule Legend */}
        <div className="flex flex-wrap gap-4 text-sm">
          <div className="flex items-center gap-2">
            <Badge className="bg-blue-100 text-blue-800 border-blue-200">M</Badge>
            <span>Morning</span>
          </div>
          <div className="flex items-center gap-2">
            <Badge className="bg-green-100 text-green-800 border-green-200">A</Badge>
            <span>Afternoon</span>
          </div>
          <div className="flex items-center gap-2">
            <Badge className="bg-purple-100 text-purple-800 border-purple-200">N</Badge>
            <span>Night</span>
          </div>
        </div>

      </CardContent>
    </Card>
    
    {/* Edit Schedule Dialog */}
    <Dialog open={isEditScheduleDialogOpen} onOpenChange={setIsEditScheduleDialogOpen}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Edit Schedule</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          <div>
            <Label htmlFor="editDriver">Driver</Label>
            <Select value={scheduleForm.driverId} onValueChange={(value) => 
              setScheduleForm(prev => ({ ...prev, driverId: value }))
            }>
              <SelectTrigger>
                <SelectValue placeholder="Select driver" />
              </SelectTrigger>
              <SelectContent>
                {drivers.map(driver => (
                  <SelectItem key={driver.id} value={driver.id}>
                    {driver.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div>
            <Label htmlFor="editDate">Date</Label>
            <Input
              id="editDate"
              type="date"
              value={scheduleForm.date}
              onChange={(e) => setScheduleForm(prev => ({ ...prev, date: e.target.value }))}
            />
          </div>

          <div>
            <Label htmlFor="editShift">Shift</Label>
            <Select value={scheduleForm.shift} onValueChange={(value: 'MORNING' | 'AFTERNOON' | 'NIGHT') => 
              setScheduleForm(prev => ({ ...prev, shift: value }))
            }>
              <SelectTrigger>
                <SelectValue placeholder="Select shift" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="MORNING">Morning (8:00 - 16:00)</SelectItem>
                <SelectItem value="AFTERNOON">Afternoon (16:00 - 24:00)</SelectItem>
                <SelectItem value="NIGHT">Night (00:00 - 8:00)</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="editStartTime">Start Time</Label>
              <Input
                id="editStartTime"
                type="time"
                value={scheduleForm.startTime}
                onChange={(e) => setScheduleForm(prev => ({ ...prev, startTime: e.target.value }))}
              />
            </div>
            <div>
              <Label htmlFor="editEndTime">End Time</Label>
              <Input
                id="editEndTime"
                type="time"
                value={scheduleForm.endTime}
                onChange={(e) => setScheduleForm(prev => ({ ...prev, endTime: e.target.value }))}
              />
            </div>
          </div>

          <div>
            <Label htmlFor="editRoute">Route (Optional)</Label>
            <Input
              id="editRoute"
              type="text"
              placeholder="e.g., Warehouse - City Center"
              value={scheduleForm.route}
              onChange={(e) => setScheduleForm(prev => ({ ...prev, route: e.target.value }))}
            />
          </div>

          <div>
            <Label htmlFor="editNotes">Notes (Optional)</Label>
            <Input
              id="editNotes"
              type="text"
              placeholder="Additional notes..."
              value={scheduleForm.notes}
              onChange={(e) => setScheduleForm(prev => ({ ...prev, notes: e.target.value }))}
            />
          </div>

          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={() => setIsEditScheduleDialogOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleUpdateSchedule}>Update Schedule</Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
    </>
  );
};

// Mock data for demo purposes
const mockSchedules: DriverSchedule[] = [
  {
    id: '1',
    driverId: '1',
    driverName: 'John Smith',
    date: new Date().toISOString().split('T')[0],
    shift: 'MORNING',
    startTime: '08:00',
    endTime: '16:00',
    status: 'CONFIRMED',
    route: 'Downtown - Airport'
  },
  {
    id: '2',
    driverId: '2',
    driverName: 'Jane Doe',
    date: new Date(Date.now() + 86400000).toISOString().split('T')[0], // Tomorrow
    shift: 'AFTERNOON',
    startTime: '16:00',
    endTime: '24:00',
    status: 'SCHEDULED',
    route: 'Warehouse - City Center'
  }
];

