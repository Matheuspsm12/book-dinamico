export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-[#EFEFEF] p-4 md:p-8">
      <div className="flex flex-col md:flex-row w-full max-w-[1090px] shadow-lg rounded-xl overflow-hidden bg-white md:min-h-[70vh] md:max-h-[90vh]">
        {/* Logo Section */}
        <div className="relative w-full md:w-1/2 flex flex-col items-center justify-center bg-zinc-100 p-6 md:p-10 min-h-[160px] md:min-h-0 flex-shrink-0">
          <img
            src="/img/logo_claro.svg"
            alt="Claro"
            className="max-h-[60px] md:max-h-[80px] w-auto object-contain"
          />
          <div className="mt-6 md:mt-0 md:absolute md:bottom-5 flex flex-col items-center">
            <img
              src="/img/logo_tcia_black.svg"
              alt="TCIA 2025"
              className="max-h-[20px] md:max-h-[25px] w-auto object-contain"
            />
            <span className="text-xs font-semibold text-zinc-700 mt-1">2025</span>
          </div>
        </div>

        {/* Form Section */}
        <div className="w-full md:w-1/2 p-5 md:p-8 lg:p-10 flex flex-col justify-center overflow-y-auto">
          {children}
        </div>
      </div>
    </div>
  );
}
