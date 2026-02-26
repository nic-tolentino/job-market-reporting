import { AlertTriangle, RefreshCw } from 'lucide-react';

interface ErrorStateProps {
    title?: string;
    message?: string;
    onRetry?: () => void;
}

export default function ErrorState({
    title = 'Something went wrong',
    message = 'We couldn\'t load this data. Please try again in a moment.',
    onRetry,
}: ErrorStateProps) {
    return (
        <div className="flex flex-col items-center justify-center min-h-[40vh] text-center px-4">
            <div className="inline-flex h-16 w-16 items-center justify-center rounded-2xl bg-red-50 border border-red-100 mb-6">
                <AlertTriangle className="h-8 w-8 text-red-500" />
            </div>
            <h2 className="text-xl font-bold text-slate-900">{title}</h2>
            <p className="mt-2 text-gray-500 max-w-md">{message}</p>
            {onRetry && (
                <button
                    onClick={onRetry}
                    className="mt-6 inline-flex items-center gap-2 px-5 py-2.5 bg-slate-900 text-white font-semibold rounded-xl hover:bg-slate-800 active:scale-[0.98] transition-all shadow-sm"
                >
                    <RefreshCw className="h-4 w-4" />
                    Try Again
                </button>
            )}
        </div>
    );
}
