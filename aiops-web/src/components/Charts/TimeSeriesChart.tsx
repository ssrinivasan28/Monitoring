import React, { useEffect, useRef, useState } from 'react';
import uPlot from 'uplot';
import 'uplot/dist/uPlot.min.css';
import { ExternalLink, RefreshCw, AlertCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { UPlotDataBundle } from '../../services/queryService';

interface TimeSeriesChartProps {
  title?: string;
  dataBundle: UPlotDataBundle;
  height?: number;
  unit?: string;
  drawStyle?: 'line' | 'bars';
  fillOpacity?: number;
  min?: number | null;
  max?: number | null;
  loading?: boolean;
  error?: string | null;
  onRefresh?: () => void;
  tenantId?: string;
  timeRangeStart?: number;
  timeRangeEnd?: number;
  filterParams?: Record<string, string>;
}

export const TimeSeriesChart: React.FC<TimeSeriesChartProps> = ({
  title,
  dataBundle,
  height = 280,
  unit = '',
  drawStyle = 'line',
  fillOpacity = 10,
  min = null,
  max = null,
  loading = false,
  error = null,
  onRefresh,
  tenantId,
  timeRangeStart,
  timeRangeEnd,
  filterParams,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<HTMLDivElement>(null);
  const uplotInstance = useRef<uPlot | null>(null);
  const navigate = useNavigate();

  const [hoverData, setHoverData] = useState<{
    timeStr: string;
    items: { label: string; value: string; color: string }[];
  } | null>(null);

  const formatValue = (val: number | null | undefined): string => {
    if (val === null || val === undefined || isNaN(val)) return 'N/A';
    if (unit === 'percent' || unit === '%') return `${val.toFixed(1)}%`;
    if (unit === 'decgbytes' || unit === 'GB') return `${val.toFixed(2)} GB`;
    if (unit === 'h' || unit === 'hours') return `${val.toFixed(1)} h`;
    return val.toLocaleString(undefined, { maximumFractionDigits: 2 });
  };

  useEffect(() => {
    if (!chartRef.current || !containerRef.current) return;

    // Clean up existing plot instance
    if (uplotInstance.current) {
      uplotInstance.current.destroy();
      uplotInstance.current = null;
    }

    const { data, series } = dataBundle;
    if (!data || data.length === 0 || !data[0] || data[0].length === 0) {
      return;
    }

    const containerWidth = containerRef.current.clientWidth || 500;

    const uplotSeries: uPlot.Series[] = [
      {
        label: 'Time',
        value: (_self, rawValue) => {
          if (!rawValue) return '--';
          const d = new Date(rawValue * 1000);
          return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
        },
      },
    ];

    series.forEach((s) => {
      const alphaHex = Math.round((fillOpacity / 100) * 255)
        .toString(16)
        .padStart(2, '0');
      const strokeColor = s.color || '#0057B8';
      const fillColor = drawStyle === 'bars' ? `${strokeColor}80` : `${strokeColor}${alphaHex}`;

      uplotSeries.push({
        label: s.label,
        stroke: strokeColor,
        fill: fillColor,
        width: 2,
        points: { show: false },
        spanGaps: false,
        value: (_self, rawValue) => formatValue(rawValue),
      });
    });

    const opts: uPlot.Options = {
      width: containerWidth,
      height: height - (title ? 48 : 24),
      title: undefined,
      cursor: {
        drag: { setScale: true },
        focus: { prox: 16 },
      },
      hooks: {
        setCursor: [
          (u) => {
            const idx = u.cursor.idx;
            if (idx !== null && idx !== undefined && u.data[0][idx] !== undefined) {
              const epoch = u.data[0][idx];
              const d = new Date(epoch * 1000);
              const timeStr = d.toLocaleDateString() + ' ' + d.toLocaleTimeString();

              const items = series.map((s, sIdx) => {
                const rawVal = u.data[sIdx + 1][idx];
                return {
                  label: s.label,
                  value: formatValue(rawVal),
                  color: s.color,
                };
              });

              setHoverData({ timeStr, items });
            } else {
              setHoverData(null);
            }
          },
        ],
      },
      scales: {
        x: { time: true },
        y: {
          auto: min === null && max === null,
          range: (min !== null || max !== null) ? [min ?? 0, max ?? 100] : undefined,
        },
      },
      axes: [
        {
          stroke: '#64748B',
          grid: { stroke: '#E2E8F0', width: 1 },
          ticks: { stroke: '#CBD5E1', width: 1 },
          font: '11px Inter, system-ui, sans-serif',
        },
        {
          stroke: '#64748B',
          grid: { stroke: '#E2E8F0', width: 1 },
          ticks: { stroke: '#CBD5E1', width: 1 },
          font: '11px Inter, system-ui, sans-serif',
          values: (_self, ticks) => ticks.map((t) => formatValue(t)),
        },
      ],
      series: uplotSeries,
    };

    const plot = new uPlot(opts, data as uPlot.AlignedData, chartRef.current);
    uplotInstance.current = plot;

    // ResizeObserver for responsive width
    const resizeObserver = new ResizeObserver((entries) => {
      if (entries[0] && uplotInstance.current) {
        const newWidth = entries[0].contentRect.width;
        if (newWidth > 0) {
          uplotInstance.current.setSize({ width: newWidth, height: height - (title ? 48 : 24) });
        }
      }
    });

    resizeObserver.observe(containerRef.current);

    return () => {
      resizeObserver.disconnect();
      if (uplotInstance.current) {
        uplotInstance.current.destroy();
        uplotInstance.current = null;
      }
    };
  }, [dataBundle, height, unit, drawStyle, fillOpacity, min, max, title]);

  const handleOpenLogExplorer = () => {
    const params = new URLSearchParams();
    if (tenantId) params.set('tenantId', tenantId);
    if (timeRangeStart) params.set('start', String(timeRangeStart));
    if (timeRangeEnd) params.set('end', String(timeRangeEnd));
    if (filterParams) {
      Object.entries(filterParams).forEach(([k, v]) => {
        if (v) params.set(k, v);
      });
    }
    const logQuery = `{app=~".+"}`;
    params.set('query', logQuery);
    navigate(`/logs?${params.toString()}`);
  };

  const hasData = dataBundle.data && dataBundle.data.length > 0 && dataBundle.data[0] && dataBundle.data[0].length > 0;

  return (
    <div
      ref={containerRef}
      className="card"
      style={{
        display: 'flex',
        flexDirection: 'column',
        position: 'relative',
        minHeight: `${height}px`,
        padding: '1rem',
      }}
    >
      {/* Header */}
      {title && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
          <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-main)', margin: 0 }}>
            {title}
          </h3>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            {onRefresh && (
              <button
                onClick={onRefresh}
                className="btn btn-secondary"
                style={{ padding: '0.25rem 0.5rem', fontSize: '0.75rem' }}
                title="Refresh chart data"
              >
                <RefreshCw size={12} className={loading ? 'spin' : ''} />
              </button>
            )}

            <button
              onClick={handleOpenLogExplorer}
              className="btn btn-secondary"
              style={{
                padding: '0.25rem 0.55rem',
                fontSize: '0.75rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.3rem',
                color: '#0057B8',
                borderColor: '#BFDBFE',
              }}
              title="Inspect logs for this time window in Log Explorer"
            >
              <ExternalLink size={12} /> Log Explorer
            </button>
          </div>
        </div>
      )}

      {/* Loading Overlay */}
      {loading && (
        <div
          style={{
            position: 'absolute',
            inset: 0,
            backgroundColor: 'rgba(255, 255, 255, 0.7)',
            zIndex: 10,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderRadius: 'var(--radius-md)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#0057B8', fontWeight: 600 }}>
            <RefreshCw size={18} className="spin" /> Loading PromQL telemetry...
          </div>
        </div>
      )}

      {/* Error State */}
      {error && !loading && (
        <div
          style={{
            padding: '1.5rem',
            backgroundColor: '#FEF2F2',
            color: '#991B1B',
            borderRadius: 'var(--radius-sm)',
            border: '1px solid #FCA5A5',
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            margin: 'auto',
          }}
        >
          <AlertCircle size={20} />
          <div>
            <div style={{ fontWeight: 700, fontSize: '0.875rem' }}>Failed to query metrics</div>
            <div style={{ fontSize: '0.75rem' }}>{error}</div>
          </div>
        </div>
      )}

      {/* Empty / No Data State */}
      {!loading && !error && !hasData && (
        <div
          style={{
            flex: 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#94A3B8',
            fontSize: '0.85rem',
            minHeight: '180px',
            border: '1px dashed #E2E8F0',
            borderRadius: 'var(--radius-sm)',
          }}
        >
          No series telemetry returned for the selected time range / filters.
        </div>
      )}

      {/* uPlot Canvas Container */}
      <div ref={chartRef} style={{ width: '100%', flex: 1, display: hasData && !error ? 'block' : 'none' }} />

      {/* Legend / Hover Table */}
      {hasData && (
        <div
          style={{
            marginTop: '0.75rem',
            paddingTop: '0.5rem',
            borderTop: '1px solid var(--border-color)',
            fontSize: '0.75rem',
            fontFamily: 'var(--font-mono)',
          }}
        >
          {hoverData && (
            <div style={{ fontSize: '0.7rem', color: '#64748B', marginBottom: '0.25rem' }}>
              Timestamp: {hoverData.timeStr}
            </div>
          )}

          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem' }}>
            {dataBundle.series.map((s, idx) => {
              const hoverItem = hoverData?.items.find((h) => h.label === s.label);
              const lastVal = dataBundle.data[idx + 1]
                ? dataBundle.data[idx + 1][dataBundle.data[idx + 1].length - 1]
                : null;
              const displayVal = hoverItem ? hoverItem.value : formatValue(lastVal);

              return (
                <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                  <span
                    style={{
                      width: '10px',
                      height: '10px',
                      borderRadius: '2px',
                      backgroundColor: s.color,
                      display: 'inline-block',
                    }}
                  />
                  <span style={{ fontWeight: 500, color: 'var(--text-main)' }}>{s.label}:</span>
                  <span className="tabular-nums" style={{ fontWeight: 700, color: '#0F172A' }}>
                    {displayVal}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};
