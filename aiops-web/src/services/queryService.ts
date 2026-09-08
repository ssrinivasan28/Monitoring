import { apiRequest } from './apiClient';

export interface PromMetric {
  [key: string]: string;
}

export interface PromMatrixSeries {
  metric: PromMetric;
  values: [number, string][]; // [epoch_seconds, string_value]
}

export interface PromVectorResult {
  metric: PromMetric;
  value: [number, string]; // [epoch_seconds, string_value]
}

export interface PromQueryResponse {
  status: string;
  data?: {
    resultType: 'matrix' | 'vector' | 'scalar' | 'string';
    result: any[];
  };
  errorType?: string;
  error?: string;
}

export interface UPlotDataBundle {
  data: [number[], ...(number | null)[][]];
  series: {
    label: string;
    metric: PromMetric;
    color: string;
  }[];
}

const BRAND_COLORS = [
  '#0057B8', // IP Blue
  '#10B981', // Emerald Green
  '#F5A300', // Amber/Orange
  '#8B5CF6', // Purple
  '#EC4899', // Pink
  '#06B6D4', // Cyan
  '#F97316', // Bright Orange
  '#6366F1', // Indigo
  '#14B8A6', // Teal
  '#EAB308', // Yellow
];

export async function queryPromqlInstant(query: string, time?: string): Promise<PromQueryResponse> {
  let url = `/api/v1/query/promql?query=${encodeURIComponent(query)}`;
  if (time) {
    url += `&time=${encodeURIComponent(time)}`;
  }
  return apiRequest<PromQueryResponse>(url);
}

export async function queryPromqlRange(
  query: string,
  start: number,
  end: number,
  step: string = '14s'
): Promise<PromQueryResponse> {
  const url = `/api/v1/query/promql/range?query=${encodeURIComponent(query)}&start=${start}&end=${end}&step=${encodeURIComponent(step)}`;
  return apiRequest<PromQueryResponse>(url);
}

export async function queryLogqlRange(
  query: string,
  start: number,
  end: number,
  step: string = '14s'
): Promise<any> {
  const url = `/api/v1/query/logql/range?query=${encodeURIComponent(query)}&start=${start}&end=${end}&step=${encodeURIComponent(step)}`;
  return apiRequest<any>(url);
}

export function formatSeriesLabel(metric: PromMetric, legendFormat?: string): string {
  if (!metric || Object.keys(metric).length === 0) {
    return 'value';
  }
  if (legendFormat) {
    let result = legendFormat;
    Object.entries(metric).forEach(([key, val]) => {
      result = result.replace(new RegExp(`{{\\s*${key}\\s*}}`, 'g'), val);
    });
    return result;
  }
  
  // Default format: display key labels (e.g., server, service, drive)
  const parts: string[] = [];
  if (metric.server) parts.push(metric.server);
  if (metric.drive) parts.push(metric.drive);
  if (metric.service) parts.push(metric.service);
  if (parts.length > 0) return parts.join(' ');

  return Object.entries(metric)
    .filter(([k]) => k !== '__name__')
    .map(([k, v]) => `${k}="${v}"`)
    .join(', ') || 'value';
}

export function transformMatrixToUPlot(
  matrixResult: PromMatrixSeries[],
  legendFormat?: string
): UPlotDataBundle {
  if (!matrixResult || matrixResult.length === 0) {
    return {
      data: [[]],
      series: [],
    };
  }

  // 1. Collect all unique timestamps across all series (sorted ascending)
  const timestampSet = new Set<number>();
  matrixResult.forEach(s => {
    s.values.forEach(([ts]) => timestampSet.add(ts));
  });

  const timestamps = Array.from(timestampSet).sort((a, b) => a - b);

  // Map timestamp to index in timestamps array
  const tsIndexMap = new Map<number, number>();
  timestamps.forEach((ts, idx) => tsIndexMap.set(ts, idx));

  // 2. Build series data arrays aligned to timestamps
  const seriesMetaData: UPlotDataBundle['series'] = [];
  const seriesValues: (number | null)[][] = [];

  matrixResult.forEach((s, idx) => {
    const label = formatSeriesLabel(s.metric, legendFormat);
    const color = BRAND_COLORS[idx % BRAND_COLORS.length];
    seriesMetaData.push({ label, metric: s.metric, color });

    const valuesArr: (number | null)[] = new Array(timestamps.length).fill(null);
    s.values.forEach(([ts, valStr]) => {
      const tsIdx = tsIndexMap.get(ts);
      if (tsIdx !== undefined) {
        const parsed = parseFloat(valStr);
        valuesArr[tsIdx] = isNaN(parsed) ? null : parsed;
      }
    });
    seriesValues.push(valuesArr);
  });

  return {
    data: [timestamps, ...seriesValues],
    series: seriesMetaData,
  };
}
