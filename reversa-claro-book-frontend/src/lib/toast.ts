import { toast } from "sonner";

export const showToast = {
  success: (message: string) => {
    toast.success(message);
  },
  error: (message: string) => {
    toast.error(message);
  },
  warning: (message: string) => {
    toast.warning(message);
  },
  info: (message: string) => {
    toast.info(message);
  },
  promise: async <T>(
    promise: Promise<T>,
    messages: { loading: string; success: string; error: (err: unknown) => string }
  ) => {
    toast.promise(promise, messages);
  },
};
