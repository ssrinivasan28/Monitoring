import React, { useEffect, useState, useCallback } from 'react';
import { LayoutDashboard, Filter, Server, HardDrive, Layers } from 'lucide-react';
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

export const WindowsMonitorDashboard: React.FC = () => {
  const { activeTenantId } = useAuth();

  // Controls & Time Range
  const [selectedOption, setSelectedOption] = useState<TimeRangeOption>(TIME_RANGE_OPTIONS[1]); // 6h default
  const [serverFilter, setServerFilter] = useState<string>('all');
  const [driveFilter, setDriveFilter] = useState<string>('all');
  const [serviceFilter, setServiceFilter] = useState<string>('all');

  // Filter option lists
  const [availableServers, setAvailableServers] = useState<string[]>([]);
  const [availableDrives, setAvailableDrives] = useState<string[]>([]);
  const [availableServices, setAvailableServices] = useState<string[]>([]);

  // State for metrics
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Overview Stats
  const [cpuStat, setCpuStat] = useState<number | null>(null);
  const [memStat, setMemStat] = useState<number | null>(null);
  const [uptimeStat, setUptimeStat] = useState<number | null>(null);
  const [hostsStat, setHostsStat] = useState<number | null>(null);

  // TimeSeries Bundles
  const [cpuTrend, setCpuTrend] = useState<UPlotDataBundle>({ data: [[]], series: [] });
  const [memTrend, setMemTrend] = useState<UPlotDataBundle>({ data: [[]], series: [] });
  const [diskPercentTrend, setDiskPercentTrend] = useState<UPlotDataBundle>({ data: [[]], series: [] });
  const [diskGbtrend, setDiskGbtrend] = useState<UPlotDataBundle>({ data: [[]], series: [] });
  const [memoryBreakdown, setMemoryBreakdown] = useState<UPlotDataBundle>({ data: [[]], series: [] });
  const [serviceTrend, setServiceTrend] = useState<UPlotDataBundle>({ data: [[]], series: [] });

  // Service Status Table
  const [serviceItems, setServiceItems] = useState<ServiceStatusItem[]>([]);

  // PromQL Filter Clauses
  const getServerRegex = () => (serverFilter === 'all' ? '.*' : serverFilter);
  const getDriveRegex = () => (driveFilter === 'all' ? '.*' : driveFilter);
  const getServiceRegex = () => (serviceFilter === 'all' ? '.*' : serviceFilter);

  const fetchDashboardData = useCallback(async () => {
    if (!activeTenantId) return;
    setLoading(true);
    setError(null);

    const now = Math.floor(Date.now() / 1000);
    const start = now - selectedOption.seconds;
    const step = selectedOption.step;

    const serverRegex = getServerRegex();
    const driveRegex = getDriveRegex();
    const serviceRegex = getServiceRegex();

    try {
      // 1. Fetch Overview Stats (Instant Queries)
      const [cpuRes, memRes, uptimeRes, hostsRes] = await Promise.all([
        queryPromqlInstant(`avg(windows_cpu_usage_percent{server=~"${serverRegex}"})`),
        queryPromqlInstant(`avg(windows_memory_usage_percent{server=~"${serverRegex}"})`),
        queryPromqlInstant(`avg(windows_system_uptime_hours{server=~"${serverRegex}"})`),
        queryPromqlInstant(`count(count by (server) (windows_cpu_usage_percent{server=~"${serverRegex}"}))`),
      ]);

      if (cpuRes.data?.result?.[0]?.value?.[1]) setCpuStat(parseFloat(cpuRes.data.result[0].value[1]));
      if (memRes.data?.result?.[0]?.value?.[1]) setMemStat(parseFloat(memRes.data.result[0].value[1]));
      if (uptimeRes.data?.result?.[0]?.value?.[1]) setUptimeStat(parseFloat(uptimeRes.data.result[0].value[1]));
      if (hostsRes.data?.result?.[0]?.value?.[1]) setHostsStat(parseFloat(hostsRes.data.result[0].value[1]));

      // 2. Fetch TimeSeries Trends (Range Queries)
      const [cpuTrendRes, memTrendRes, diskPctRes, diskGbUsedRes, diskGbTotalRes, memUsedRes, memFreeRes, memTotalRes, serviceTrendRes, serviceTableRes] =
        await Promise.all([
          queryPromqlRange(`windows_cpu_usage_percent{server=~"${serverRegex}"}`, start, now, step),
          queryPromqlRange(`windows_memory_usage_percent{server=~"${serverRegex}"}`, start, now, step),
          queryPromqlRange(`windows_disk_usage_percent{server=~"${serverRegex}",drive=~"${driveRegex}"}`, start, now, step),
          queryPromqlRange(`windows_disk_used_gb{server=~"${serverRegex}",drive=~"${driveRegex}"}`, start, now, step),
          queryPromqlRange(`windows_disk_total_gb{server=~"${serverRegex}",drive=~"${driveRegex}"}`, start, now, step),
          queryPromqlRange(`windows_memory_used_gb{server=~"${serverRegex}"}`, start, now, step),
          queryPromqlRange(`windows_memory_free_gb{server=~"${serverRegex}"}`, start, now, step),
          queryPromqlRange(`windows_memory_total_gb{server=~"${serverRegex}"}`, start, now, step),
          queryPromqlRange(`windows_service_status{server=~"${serverRegex}",service=~"${serviceRegex}"}`, start, now, step),
          queryPromqlInstant(`windows_service_status{server=~"${serverRegex}",service=~"${serviceRegex}"}`),
        ]);

      // Transform PromQL matrix responses into uPlot bundles
      if (cpuTrendRes.data?.result) setCpuTrend(transformMatrixToUPlot(cpuTrendRes.data.result, '{{server}}'));
      if (memTrendRes.data?.result) setMemTrend(transformMatrixToUPlot(memTrendRes.data.result, '{{server}}'));
      if (diskPctRes.data?.result) setDiskPercentTrend(transformMatrixToUPlot(diskPctRes.data.result, '{{server}} {{drive}}'));

      // Combine Disk Used + Total into single chart bundle
      const combinedDisk: any[] = [
        ...(diskGbUsedRes.data?.result || []).map((r: any) => ({ ...r, metric: { ...r.metric, __legend: `${r.metric.server || ''} ${r.metric.drive || ''} Used` } })),
        ...(diskGbTotalRes.data?.result || []).map((r: any) => ({ ...r, metric: { ...r.metric, __legend: `${r.metric.server || ''} ${r.metric.drive || ''} Total` } })),
      ];
      setDiskGbtrend(transformMatrixToUPlot(combinedDisk, '{{__legend}}'));

      // Combine Memory Used / Free / Total into single chart bundle
      const combinedMem: any[] = [
        ...(memUsedRes.data?.result || []).map((r: any) => ({ ...r, metric: { ...r.metric, __legend: `${r.metric.server || ''} Used` } })),
        ...(memFreeRes.data?.result || []).map((r: any) => ({ ...r, metric: { ...r.metric, __legend: `${r.metric.server || ''} Free` } })),
        ...(memTotalRes.data?.result || []).map((r: any) => ({ ...r, metric: { ...r.metric, __legend: `${r.metric.server || ''} Total` } })),
      ];
      setMemoryBreakdown(transformMatrixToUPlot(combinedMem, '{{__legend}}'));

      if (serviceTrendRes.data?.result) setServiceTrend(transformMatrixToUPlot(serviceTrendRes.data.result, '{{server}} {{service}}'));

      // Build Service Status Table Items
      if (serviceTableRes.data?.result) {
        const items: ServiceStatusItem[] = serviceTableRes.data.result.map((r: any) => ({
          server: r.metric.server || 'Unknown Host',
          service: r.metric.service || 'Unknown Service',
          statusValue: parseFloat(r.value[1]),
        }));
        setServiceItems(items);

        // Update available filter option lists dynamically
        const servers = Array.from(new Set(items.map((i) => i.server)));
        const services = Array.from(new Set(items.map((i) => i.service)));
        setAvailableServers(servers);
        setAvailableServices(services);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to execute gateway dashboard queries');
    } finally {
      setLoading(false);
    }
  }, [activeTenantId, selectedOption, serverFilter, driveFilter, serviceFilter]);

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
          <LayoutDashboard color="#0057B8" size={26} /> Windows Monitor (uPlot Parity)
        </h1>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          Side-by-side Grafana panel parity for host performance, capacity, and service telemetry via SOC 2 query gateway.
        </p>
      </div>

      {/* Global Time & Tenant Bar */}
      <TimeRangeSelector
        selectedRangeSeconds={selectedOption.seconds}
        onRangeChange={(opt) => setSelectedOption(opt)}
        onManualRefresh={fetchDashboardData}
        loading={loading}
      />

      {/* Dashboard Filter Bar */}
      <div
        className="card"
        style={{
          padding: '0.75rem 1rem',
          marginBottom: '1.5rem',
          display: 'flex',
          alignItems: 'center',
          gap: '1.5rem',
          flexWrap: 'wrap',
          fontSize: '0.85rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 700, color: 'var(--text-main)' }}>
          <Filter size={16} color="#0057B8" /> Panel Filters:
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <Server size={14} color="#64748B" />
          <span style={{ color: 'var(--text-muted)' }}>Location / Server:</span>
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

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <HardDrive size={14} color="#64748B" />
          <span style={{ color: 'var(--text-muted)' }}>Drive:</span>
          <select
            value={driveFilter}
            onChange={(e) => setDriveFilter(e.target.value)}
            style={{ padding: '0.25rem 0.5rem', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)', fontSize: '0.8rem' }}
          >
            <option value="all">All Drives</option>
            <option value="C:">C:</option>
            <option value="D:">D:</option>
            <option value="E:">E:</option>
          </select>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <Layers size={14} color="#64748B" />
          <span style={{ color: 'var(--text-muted)' }}>Service:</span>
          <select
            value={serviceFilter}
            onChange={(e) => setServiceFilter(e.target.value)}
            style={{ padding: '0.25rem 0.5rem', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)', fontSize: '0.8rem' }}
          >
            <option value="all">All Services</option>
            {availableServices.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Row 1: Overview (Stats) */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1.25rem', marginBottom: '1.5rem' }}>
        <StatCard
          title="CPU Usage"
          value={cpuStat}
          unit="percent"
          thresholds={[
            { value: null, color: 'green' },
            { value: 75, color: 'orange' },
            { value: 90, color: 'red' },
          ]}
          loading={loading}
          error={error}
        />
        <StatCard
          title="Memory Usage"
          value={memStat}
          unit="percent"
          thresholds={[
            { value: null, color: 'green' },
            { value: 75, color: 'orange' },
            { value: 90, color: 'red' },
          ]}
          loading={loading}
          error={error}
        />
        <StatCard
          title="System Uptime"
          value={uptimeStat}
          unit="h"
          thresholds={[
            { value: null, color: 'green' },
            { value: 24, color: 'orange' },
            { value: 168, color: 'red' },
          ]}
          loading={loading}
          error={error}
        />
        <StatCard
          title="Hosts Selected"
          value={hostsStat}
          unit="short"
          decimals={0}
          thresholds={[
            { value: null, color: 'red' },
            { value: 1, color: 'green' },
          ]}
          loading={loading}
          error={error}
        />
      </div>

      {/* Row 2: Usage Trends (TimeSeries) */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(500px, 1fr))', gap: '1.25rem', marginBottom: '1.5rem' }}>
        <TimeSeriesChart
          title="CPU Usage Over Time"
          dataBundle={cpuTrend}
          unit="percent"
          min={0}
          max={100}
          loading={loading}
          error={error}
          onRefresh={fetchDashboardData}
          tenantId={activeTenantId || undefined}
          timeRangeStart={timeRangeStart}
          timeRangeEnd={now}
        />
        <TimeSeriesChart
          title="Memory Usage Over Time"
          dataBundle={memTrend}
          unit="percent"
          min={0}
          max={100}
          loading={loading}
          error={error}
          onRefresh={fetchDashboardData}
          tenantId={activeTenantId || undefined}
          timeRangeStart={timeRangeStart}
          timeRangeEnd={now}
        />
      </div>

      {/* Row 3: Capacity (TimeSeries/Bars) */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(500px, 1fr))', gap: '1.25rem', marginBottom: '1.5rem' }}>
        <TimeSeriesChart
          title="Disk Usage Percent"
          dataBundle={diskPercentTrend}
          drawStyle="bars"
          fillOpacity={80}
          unit="percent"
          min={0}
          max={100}
          loading={loading}
          error={error}
          onRefresh={fetchDashboardData}
          tenantId={activeTenantId || undefined}
          timeRangeStart={timeRangeStart}
          timeRangeEnd={now}
        />
        <TimeSeriesChart
          title="Disk Used vs Total"
          dataBundle={diskGbtrend}
          drawStyle="bars"
          fillOpacity={70}
          unit="decgbytes"
          loading={loading}
          error={error}
          onRefresh={fetchDashboardData}
          tenantId={activeTenantId || undefined}
          timeRangeStart={timeRangeStart}
          timeRangeEnd={now}
        />
      </div>

      {/* Row 4: Memory Breakdown */}
      <div style={{ marginBottom: '1.5rem' }}>
        <TimeSeriesChart
          title="Memory Used / Free / Total"
          dataBundle={memoryBreakdown}
          drawStyle="bars"
          fillOpacity={80}
          unit="decgbytes"
          loading={loading}
          error={error}
          onRefresh={fetchDashboardData}
          tenantId={activeTenantId || undefined}
          timeRangeStart={timeRangeStart}
          timeRangeEnd={now}
        />
      </div>

      {/* Row 5: Services (Table & TimeSeries) */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(500px, 1fr))', gap: '1.25rem', marginBottom: '2rem' }}>
        <StatusTable
          title="Service Status Table"
          items={serviceItems}
          loading={loading}
          error={error}
          onRefresh={fetchDashboardData}
          tenantId={activeTenantId || undefined}
        />
        <TimeSeriesChart
          title="Service Status Over Time"
          dataBundle={serviceTrend}
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
