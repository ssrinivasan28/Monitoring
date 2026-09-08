import React, { useEffect, useState, useCallback } from 'react';
import { Server, Activity, Filter } from 'lucide-react';
import {
  queryPromqlInstant,
  queryPromqlRange,
  transformMatrixToUPlot,
  UPlotDataBundle,
} from '../../services/queryService';
import { StatCard } from '../../components/Charts/StatCard';
import { TimeSeriesChart } from '../../components/Charts/TimeSeriesChart';
import { StatusTable, ServiceStatusItem } from '../../components/Charts/StatusTable';
import {
  TimeRangeSelector,
  TIME_RANGE_OPTIONS,
  TimeRangeOption,
} from '../../components/Charts/TimeRangeSelector';
import { useAuth } from '../../context/AuthContext';

export const WinServiceMonitorDashboard: React.FC = () => {
  const { activeTenantId } = useAuth();

  // Controls & Time Range
  const [selectedOption, setSelectedOption] = useState<TimeRangeOption>(TIME_RANGE_OPTIONS[0]); // 1h default
  const [serverFilter, setServerFilter] = useState<string>('all');
  const [availableServers, setAvailableServers] = useState<string[]>([]);

  // Loading & Error States
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Overview Stats & Down Cycles
  const [servicesOverviewStat, setServicesOverviewStat] = useState<number | null>(null);
  const [downCyclesStat, setDownCyclesStat] = useState<number | null>(null);

  // Status Table Items
  const [serviceTableItems, setServiceTableItems] = useState<ServiceStatusItem[]>([]);

  // TimeSeries History Bundle
  const [historyTrend, setHistoryTrend] = useState<UPlotDataBundle>({ data: [[]], series: [] });

  const getServerRegex = () => (serverFilter === 'all' ? '.*' : serverFilter);

  const fetchDashboardData = useCallback(async () => {
    if (!activeTenantId) return;
    setLoading(true);
    setError(null);

    const now = Math.floor(Date.now() / 1000);
    const start = now - selectedOption.seconds;
    const step = selectedOption.step;

    const serverRegex = getServerRegex();

    try {
      // 1. Fetch Instant Queries (Overview, Down Cycles, Status Table)
      const [allStatusRes, downCyclesRes, tableRes] = await Promise.all([
        queryPromqlInstant(`win_service_status{server=~"${serverRegex}"}`),
        queryPromqlInstant(`win_service_down_cycles{server=~"${serverRegex}"} > 0`),
        queryPromqlInstant(`win_service_status{server=~"${serverRegex}"}`),
      ]);

      // Calculate aggregate overall status (1 if all running, 0 if any down)
      if (allStatusRes.data?.result) {
        const statuses = allStatusRes.data.result.map((r: any) => parseFloat(r.value[1]));
        if (statuses.length > 0) {
          const allRunning = statuses.every((s: number) => s === 1);
          setServicesOverviewStat(allRunning ? 1 : 0);
        } else {
          setServicesOverviewStat(null);
        }
      }

      // Count down cycles
      if (downCyclesRes.data?.result) {
        const downCount = downCyclesRes.data.result.length;
        setDownCyclesStat(downCount);
      } else {
        setDownCyclesStat(0);
      }

      // Populate Service Table items
      if (tableRes.data?.result) {
        const items: ServiceStatusItem[] = tableRes.data.result.map((r: any) => ({
          server: r.metric.server || 'Unknown Server',
          service: r.metric.service || 'Unknown Service',
          statusValue: parseFloat(r.value[1]),
        }));
        setServiceTableItems(items);

        const servers = Array.from(new Set(items.map((i) => i.server)));
        setAvailableServers(servers);
      }

      // 2. Fetch TimeSeries Range Query (Service Status History)
      const historyRes = await queryPromqlRange(`win_service_status{server=~"${serverRegex}"}`, start, now, step);
      if (historyRes.data?.result) {
        setHistoryTrend(transformMatrixToUPlot(historyRes.data.result, '{{server}} — {{service}}'));
      }
    } catch (err: any) {
      setError(err.message || 'Failed to execute Windows Service Monitor queries');
    } finally {
      setLoading(false);
    }
  }, [activeTenantId, selectedOption, serverFilter]);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  const now = Math.floor(Date.now() / 1000);
  const timeRangeStart = now - selectedOption.seconds;

  return (
    <div style={{ maxWidth: '1400px', margin: '0 auto' }}>
      {/* Page Header */}
      <div style={{ marginBottom: '1.25rem' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <Activity color="#0057B8" size={26} /> Windows Service Monitor (uPlot Parity)
        </h1>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          Detailed Windows service state monitoring, alert down-cycle windowing, and history telemetry.
        </p>
      </div>

      {/* Global Time & Tenant Selector */}
      <TimeRangeSelector
        selectedRangeSeconds={selectedOption.seconds}
        onRangeChange={(opt) => setSelectedOption(opt)}
        onManualRefresh={fetchDashboardData}
        loading={loading}
      />

      {/* Server Filter Dropdown */}
      <div
        className="card"
        style={{
          padding: '0.75rem 1rem',
          marginBottom: '1.5rem',
          display: 'flex',
          alignItems: 'center',
          gap: '1rem',
          fontSize: '0.85rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 700, color: 'var(--text-main)' }}>
          <Filter size={16} color="#0057B8" /> Server Filter:
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <Server size={14} color="#64748B" />
          <select
            value={serverFilter}
            onChange={(e) => setServerFilter(e.target.value)}
            style={{ padding: '0.25rem 0.5rem', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)', fontSize: '0.8rem' }}
          >
            <option value="all">All Servers</option>
            {availableServers.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Row 1 & 3: Overview & Alert Down Cycles Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.25rem', marginBottom: '1.5rem' }}>
        <StatCard
          title="Service Status Overview"
          value={servicesOverviewStat}
          mappings={[
            { value: 0, text: 'DOWN', color: 'red' },
            { value: 1, text: 'Running', color: 'green' },
          ]}
          subtitle="Aggregate health across monitored Windows services"
          loading={loading}
          error={error}
        />
        <StatCard
          title="Services Currently Down (Consecutive Cycles)"
          value={downCyclesStat}
          unit="short"
          decimals={0}
          thresholds={[
            { value: null, color: 'green' },
            { value: 1, color: 'orange' },
            { value: 2, color: 'red' },
          ]}
          subtitle="Services exceeding alert window threshold"
          loading={loading}
          error={error}
        />
      </div>

      {/* Row 2: Per Server Status Table */}
      <div style={{ marginBottom: '1.5rem' }}>
        <StatusTable
          title="Service Status Table"
          items={serviceTableItems}
          loading={loading}
          error={error}
          onRefresh={fetchDashboardData}
          tenantId={activeTenantId || undefined}
        />
      </div>

      {/* Row 4: History (TimeSeries) */}
      <div style={{ marginBottom: '2rem' }}>
        <TimeSeriesChart
          title="Service Status Over Time"
          dataBundle={historyTrend}
          unit="short"
          min={0}
          max={1}
          loading={loading}
          error={error}
          onRefresh={fetchDashboardData}
          tenantId={activeTenantId || undefined}
          timeRangeStart={timeRangeStart}
          timeRangeEnd={now}
        />
      </div>
    </div>
  );
};
