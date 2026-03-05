import { Loader2 } from 'lucide-react';

export default function PageLoader() {
    return (
        <div className="flex items-center justify-center min-h-[50vh]">
            <Loader2 className="w-8 h-8 text-accent animate-spin" />
        </div>
    );
}
