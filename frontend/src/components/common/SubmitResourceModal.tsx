import { useState, useRef, useEffect } from 'react';
import { PlusCircle, X, Send, AlertCircle } from 'lucide-react';
import { submitFeedback } from '../../lib/api';
import { useMutation } from '@tanstack/react-query';

interface SubmitResourceModalProps {
    isOpen: boolean;
    onClose: () => void;
    techName: string;
    contextType: 'Community' | 'Learning';
}

export function SubmitResourceModal({ isOpen, onClose, techName, contextType }: SubmitResourceModalProps) {
    const [status, setStatus] = useState<'idle' | 'submitting' | 'success'>('idle');
    const [url, setUrl] = useState('');
    const [category, setCategory] = useState('Website');
    const [notes, setNotes] = useState('');
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
        // For now, we tunnel the unstructured resource submission through the generic feedback API
        mutationFn: (message: string) => submitFeedback(`Resource Submission: ${techName} (${contextType})`, message),
        onSuccess: () => {
            setStatus('success');
            setTimeout(() => {
                onClose();
                setStatus('idle');
                setUrl('');
                setNotes('');
            }, 2000);
        },
        onError: () => {
            setStatus('idle');
            alert("Failed to submit resource. Please try again.");
        }
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        setStatus('submitting');
        const payload = `URL: ${url}\nCategory: ${category}\nNotes: ${notes}`;
        mutate(payload);
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm animate-in fade-in duration-200">
            <div
                ref={modalRef}
                className="w-full max-w-md bg-card rounded-2xl shadow-2xl border border-indigo-200 dark:border-indigo-500/30 overflow-hidden transform animate-in zoom-in-95 duration-300"
            >
                <div className="bg-indigo-50 dark:bg-indigo-500/10 px-6 py-4 border-b border-indigo-100 dark:border-indigo-500/20 flex justify-between items-center">
                    <div className="flex items-center gap-2">
                        <div className="p-1.5 bg-indigo-100 dark:bg-indigo-500/20 rounded-lg">
                            <PlusCircle className="h-4 w-4 text-indigo-700 dark:text-indigo-400" />
                        </div>
                        <h3 className="font-bold text-primary text-lg">Suggest a Resource</h3>
                    </div>
                    <button onClick={onClose} className="p-2 hover:bg-indigo-200/50 dark:hover:bg-indigo-500/20 rounded-full transition-colors">
                        <X className="h-5 w-5 text-indigo-500 dark:text-indigo-400" />
                    </button>
                </div>

                <div className="p-6">
                    {status === 'success' ? (
                        <div className="py-8 text-center space-y-4">
                            <div className="inline-flex h-16 w-16 items-center justify-center rounded-full bg-green-100 dark:bg-green-500/20 text-green-600 dark:text-green-400">
                                <Send className="h-8 w-8" />
                            </div>
                            <div>
                                <h4 className="text-xl font-bold text-primary">Resource Received!</h4>
                                <p className="text-muted mt-1">Our moderation team will review it shortly to add to the {techName} index. Thank you!</p>
                            </div>
                        </div>
                    ) : (
                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div className="flex items-start gap-2.5 p-3 rounded-xl bg-orange-50/50 dark:bg-orange-500/10 border border-orange-100 dark:border-orange-500/20 mb-4">
                                <AlertCircle className="h-4 w-4 text-orange-500 mt-0.5" />
                                <div>
                                    <p className="text-xs font-bold text-orange-600 dark:text-orange-400 uppercase tracking-wider">Help grow the community</p>
                                    <p className="text-sm text-orange-900 dark:text-orange-300 font-medium leading-snug mt-0.5">Know a great Youtube Channel, Open Source Project, or Course? Paste it below!</p>
                                </div>
                            </div>

                            <div className="space-y-3">
                                <div>
                                    <label className="block text-xs font-bold text-secondary mb-1">Resource URL</label>
                                    <input
                                        required
                                        type="url"
                                        value={url}
                                        onChange={(e) => setUrl(e.target.value)}
                                        placeholder="https://..."
                                        className="w-full px-4 py-2 bg-elevated border border-border rounded-xl text-sm text-primary focus:outline-none focus:ring-2 focus:ring-accent/20 transition-all font-medium placeholder:text-muted"
                                    />
                                </div>
                                <div className="grid grid-cols-2 gap-3">
                                    <div>
                                        <label className="block text-xs font-bold text-secondary mb-1">Category</label>
                                        <select
                                            value={category}
                                            onChange={(e) => setCategory(e.target.value)}
                                            className="w-full bg-elevated border border-border rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-accent/20 transition-all font-medium text-secondary appearance-none bg-[url('data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20fill%3D%22none%22%20viewBox%3D%220%200%2020%2020%22%3E%3Cpath%20stroke%3D%22%236b7280%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%20stroke-width%3D%221.5%22%20d%3D%22m6%208%204%204%204-4%22%2F%3E%3C%2Fsvg%3E')] bg-[length:1.25em_1.25em] bg-[right_0.5rem_center] bg-no-repeat pr-8 dark:[filter:invert(1)]"
                                        >
                                            {contextType === 'Learning' ? (
                                                <>
                                                    <option>Youtube Channel</option>
                                                    <option>Course / Tutorial</option>
                                                    <option>Podcast</option>
                                                    <option>Website / Blog</option>
                                                    <option>Open Source Project</option>
                                                </>
                                            ) : (
                                                <>
                                                    <option>Local Community Group</option>
                                                    <option>Upcoming Event / Meetup</option>
                                                    <option>Local Open Source Project</option>
                                                    <option>Community Expert</option>
                                                </>
                                            )}
                                        </select>
                                    </div>
                                    <div>
                                        <label className="block text-xs font-bold text-secondary mb-1">Technology</label>
                                        <div className="w-full px-4 py-2 bg-inset border border-border rounded-xl text-sm font-bold text-muted cursor-not-allowed">
                                            {techName}
                                        </div>
                                    </div>
                                </div>
                                <div>
                                    <label className="block text-xs font-bold text-secondary mb-1">Additional Notes (Optional)</label>
                                    <textarea
                                        value={notes}
                                        onChange={(e) => setNotes(e.target.value)}
                                        placeholder="Why is it great? Any specific context?"
                                        className="w-full h-16 px-4 py-2 rounded-xl border border-border bg-elevated focus:border-accent focus:ring-2 focus:ring-accent/20 transition-all outline-none text-sm text-primary resize-none placeholder:text-muted"
                                    />
                                </div>
                            </div>

                            <button
                                type="submit"
                                disabled={isPending || status === 'submitting'}
                                className="w-full mt-2 py-3 bg-indigo-600 text-white font-bold rounded-xl hover:bg-indigo-700 shadow-md shadow-indigo-200 dark:shadow-indigo-900/30 hover:shadow-lg hover:-translate-y-0.5 active:translate-y-0 active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-50"
                            >
                                {isPending || status === 'submitting' ? (
                                    <div className="h-5 w-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                                ) : (
                                    <>
                                        Submit for Review
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
