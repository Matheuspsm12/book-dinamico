export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[#EFEFEF] p-4 md:p-8">
      <div className="flex w-full max-w-[1090px] flex-col overflow-hidden rounded-xl bg-white shadow-lg md:max-h-[90vh] md:min-h-[70vh] md:flex-row">
        <div className="relative flex min-h-[160px] w-full flex-shrink-0 flex-col items-center justify-center bg-zinc-100 p-6 md:min-h-0 md:w-1/2 md:p-10">
          <img
            src="/img/logo_claro.svg"
            alt="Claro"
            className="max-h-[60px] w-auto object-contain md:max-h-[80px]"
          />
          <div className="mt-6 flex flex-col items-center md:absolute md:bottom-5 md:mt-0">
            <img
              src="/img/logo_tcia_black.svg"
              alt="TCIA 2025"
              className="max-h-[20px] w-auto object-contain md:max-h-[25px]"
            />
            <span className="mt-1 font-semibold text-xs text-zinc-700">
              2025
            </span>
          </div>
        </div>

        <div className="flex w-full flex-col justify-center overflow-y-auto p-5 md:w-1/2 md:p-8 lg:p-10">
          {children}
        </div>
      </div>
    </div>
  );
}
