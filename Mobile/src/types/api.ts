export type UsuarioRole = "ADMIN" | "OPERADOR" | (string & {});

export interface TokenResponse {
  token: string;
  expiraEm: string;
  nome: string;
  email: string;
  role: UsuarioRole;
  producao: boolean;
}

export type AuthSession = TokenResponse;

export type TipoDocumento = "POWERPOINT" | "EXCEL";
export type ExtensaoDocumento =
  | "XLSM"
  | "XLSX"
  | "XLSB"
  | "XLTX"
  | "XLTM"
  | "PPTX";

export interface DocumentoResponse {
  id: number;
  nome: string;
  descricao: string;
  tipo: TipoDocumento;
  extensao: ExtensaoDocumento;
  tamanhoBytes: number;
  dataAtualizacao: string;
  criadoEm: string;
  atualizadoEm: string;
  criadoPorId: number;
  atualizadoPorId: number;
  ativo: boolean;
}

export interface ApiErrorBody {
  message?: string;
  error?: string;
}

export interface PageResponse<T> {
  totalElements: number;
  content: T[];
}

export interface UsuarioResponse {
  id: number;
  nome: string;
  email: string;
  status: string;
}
