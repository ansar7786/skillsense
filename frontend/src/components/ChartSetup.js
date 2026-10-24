import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  ArcElement,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';

let registered = false;

export function ensureChartRegistered() {
  if (!registered) {
    ChartJS.register(
      CategoryScale,
      LinearScale,
      BarElement,
      ArcElement,
      PointElement,
      LineElement,
      Title,
      Tooltip,
      Legend
    );
    registered = true;
  }
}

// Fixed color-blind friendly 6-color palette
export const CHART_COLORS = [
  '#4f46e5', // Indigo
  '#0284c7', // Sky Blue
  '#059669', // Emerald
  '#d97706', // Amber
  '#e11d48', // Rose
  '#7c3aed', // Violet
];

export const STATUS_COLORS = {
  READY: '#16a34a',
  DEVELOPING: '#d97706',
  NEEDS_SUPPORT: '#dc2626',
};
