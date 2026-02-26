export default function Footer() {
    return (
        <footer className="mt-auto border-t border-gray-200 bg-white py-8">
            <div className="mx-auto max-w-6xl px-4 pb-4 sm:px-6 lg:px-8">
                <div className="flex flex-col items-center justify-between gap-4 sm:flex-row">
                    <p className="text-sm text-gray-500">
                        &copy; {new Date().getFullYear()} JobMarket Pulse. Data extracted automatically for educational insights.
                    </p>
                    <div className="flex gap-4 text-sm text-gray-500">
                        <a href="#" className="hover:text-gray-900 transition-colors">Privacy Policy</a>
                        <a href="#" className="hover:text-gray-900 transition-colors">Terms of Service</a>
                    </div>
                </div>
            </div>
        </footer>
    );
}
