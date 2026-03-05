import { useState, useRef, useEffect } from 'react';
import { MessageSquare, X, Send, AlertCircle } from 'lucide-react';
import { submitFeedback } from '../../lib/api';
import { useMutation } from '@tanstack/react-query';

interface FeedbackModalProps {
    isOpen: boolean;
    onClose: () => void;
    context?: string;
}

export function FeedbackModal({ isOpen, onClose, context }: FeedbackModalProps) {
    const [status, setStatus] = useState<'idle' | 'submitting' | 'success'>('idle');
    const [feedback, setFeedback] = useState('');
    const modalRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (modalRef.current && !modalRef.current.contains(event.target as Node)) {
                onClose();
            }
        }
        if (isOpen) {
            document.addEventListener('mousedown', handleClickOutside);
        }
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [isOpen, onClose]);

    const { mutate, isPending } = useMutation({
        mutationFn: (message: string) => submitFeedback(context, message),
        onSuccess: () => {
            setStatus('success');
            setTimeout(() => {
                onClose();
                setStatus('idle');
                setFeedback('');
            }, 2000);
        },
        onError: () => {
            // Revert back or show error
            setStatus('idle');
            alert("Failed to submit feedback. Please try again.");
        }
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        setStatus('submitting');
        mutate(feedback);
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-in fade-in duration-200">
            <div
                ref={modalRef}
                className="w-full max-w-md bg-card rounded-2xl shadow-2xl border border-border overflow-hidden transform animate-in slide-in-from-bottom-4 duration-300"
            >
                <div className="bg-elevated px-6 py-4 border-b border-border-subtle flex justify-between items-center">
                    <div className="flex items-center gap-2">
                        <div className="p-1.5 bg-blue-100 dark:bg-blue-500/20 rounded-lg">
                            <MessageSquare className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                        </div>
                        <h3 className="font-bold text-primary text-lg">Send Feedback</h3>
                    </div>
                    <button onClick={onClose} className="p-2 hover:bg-surface-hover rounded-full transition-colors">
                        <X className="h-5 w-5 text-muted" />
                    </button>
                </div>

                <div className="p-6">
                    {status === 'success' ? (
                        <div className="py-8 text-center space-y-4">
                            <div className="inline-flex h-16 w-16 items-center justify-center rounded-full bg-green-100 dark:bg-green-500/20 text-green-600 dark:text-green-400">
                                <Send className="h-8 w-8" />
                            </div>
                            <div>
                                <h4 className="text-xl font-bold text-primary">Thank you!</h4>
                                <p className="text-muted mt-1">Your feedback helps us improve our market insights.</p>
                            </div>
                        </div>
                    ) : (
                        <form onSubmit={handleSubmit} className="space-y-4">
                            {context && (
                                <div className="flex items-start gap-2.5 p-3 rounded-xl bg-accent-subtle border border-accent/20 mb-4">
                                    <AlertCircle className="h-4 w-4 text-accent mt-0.5" />
                                    <div>
                                        <p className="text-xs font-bold text-accent uppercase tracking-wider">Context</p>
                                        <p className="text-sm text-primary font-medium">{context}</p>
                                    </div>
                                </div>
                            )}

                            <div>
                                <label className="block text-sm font-bold text-secondary mb-1.5">What can we improve?</label>
                                <textarea
                                    required
                                    value={feedback}
                                    onChange={(e) => setFeedback(e.target.value)}
                                    placeholder="Tell us about incorrect data, missing features, or bugs..."
                                    className="w-full h-32 px-4 py-3 rounded-xl border border-border bg-card focus:border-accent focus:ring-2 focus:ring-accent/20 transition-all outline-none text-sm text-primary resize-none placeholder:text-muted"
                                />
                            </div>

                            <button
                                type="submit"
                                disabled={isPending || status === 'submitting'}
                                className="w-full py-3 bg-primary text-inverted font-bold rounded-xl hover:opacity-90 active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-50"
                            >
                                {isPending || status === 'submitting' ? (
                                    <div className="h-5 w-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                                ) : (
                                    <>
                                        Submit Feedback
                                        <Send className="h-4 w-4" />
                                    </>
                                )}
                            </button>
                        </form>
                    )}
                </div>
            </div>
        </div>
    );
}

export function FeedbackButton({ context, variant = 'text' }: { context?: string, variant?: 'text' | 'icon' }) {
    const [isOpen, setIsOpen] = useState(false);

    if (variant === 'icon') {
        return (
            <>
                <button
                    onClick={() => setIsOpen(true)}
                    className="p-1.5 text-muted hover:text-accent hover:bg-accent-subtle rounded-lg transition-all"
                    title="Provide feedback"
                >
                    <MessageSquare className="h-4 w-4" />
                </button>
                <FeedbackModal isOpen={isOpen} onClose={() => setIsOpen(false)} context={context} />
            </>
        );
    }

    return (
        <>
            <button
                onClick={() => setIsOpen(true)}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold text-muted hover:text-accent hover:bg-accent-subtle rounded-lg transition-all border border-transparent hover:border-accent/20 group"
            >
                <MessageSquare className="h-3.5 w-3.5" />
                Provide feedback on this data
            </button>
            <FeedbackModal isOpen={isOpen} onClose={() => setIsOpen(false)} context={context} />
        </>
    );
}
