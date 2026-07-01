export type UsuarioStatus =
  | "PENDENTE"
  | "APROVADO"
  | "REJEITADO"
  | "DESATIVADO";
export type UsuarioRole = "ADMIN" | "USUARIO";

export type TipoDocumento = "POWERPOINT" | "EXCEL";
export type ExtensaoDocumento = "XLSM" | "XLSX" | "PPTX";

export interface TokenResponse {
  token: string;
  expiraEm: string;
  nome: string;
  email: string;
  role: UsuarioRole;
}

export interface UsuarioResponse {
  id: number;
  nome: string;
  empresa: string;
  email: string;
  justificativa?: string;
  status: UsuarioStatus;
  role: UsuarioRole;
  criadoEm?: string;
  atualizadoEm?: string;
  decididoEm?: string;
  aprovadoPorId?: number;
}

export interface UsuarioCadastroRequest {
  nome: string;
  empresa: string;
  email: string;
  senha: string;
  justificativa: string;
}

export interface UsuarioEdicaoRequest {
  nome?: string;
  empresa?: string;
  email?: string;
}

export interface UsuarioFiltroRequest {
  status?: UsuarioStatus;
  empresa?: string;
  nome?: string;
}

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

export interface DocumentoMetadataRequest {
  nome: string;
  descricao: string;
  dataAtualizacao: string;
}

export interface DocumentoAbaResponse {
  nomeAba: string;
  qtdLinhas: number;
  qtdColunas: number;
}

export type ProcessamentoTipo = "DOCUMENTO";

export interface ProcessamentoResponse {
  id: number;
  nomeArquivo: string;
  tipoProcessamento: ProcessamentoTipo | string;
  nomeProcessamento?: string;
  resultadoAmigavel?: string;
  tamanho?: string;
  usuario?: string;
  documentoId?: number;
  dataStart?: string;
  dataInicio?: string;
  dataFim?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
