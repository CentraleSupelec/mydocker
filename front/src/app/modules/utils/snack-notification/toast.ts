export type ToastType = 'error' | 'success' | 'info';

export interface ISnackData {
  message: string;
  type: ToastType;
}
