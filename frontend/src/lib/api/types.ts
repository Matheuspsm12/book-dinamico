export type UsuarioStatus =
  | "PENDENTE"
  | "APROVADO"
  | "REJEITADO"
  | "DESATIVADO";
// Perfis do sistema: ADMIN (gate de segurança/guards), OPERADOR (default do
// autocadastro) e LOGISTICA. (string & {}) mantém tolerância a valores futuros.
export type UsuarioRole = "ADMIN" | "OPERADOR" | "LOGISTICA" | (string & {});

export interface PerfilResponse {
  id: number;
  nomePerfil: string;
  descricao?: string;
  ativado?: boolean;
}

export type TipoDocumento = "POWERPOINT" | "EXCEL";
export type ExtensaoDocumento = "XLSM" | "XLSX" | "PPTX";

export interface TokenResponse {
  token: string;
  expiraEm: string;
  nome: string;
  email: string;
  role: UsuarioRole;
  producao: boolean;
}

export interface UsuarioResponse {
  id: number;
  nome: string;
  empresa: string;
  email: string;
  justificativa?: string;
  status: UsuarioStatus;
  role: UsuarioRole;
  idPerfil?: number;
  criadoEm?: string;
  atualizadoEm?: string;
  decididoEm?: string;
  aprovadoPorId?: number;
  ultimoAcesso?: string;
  ociosidadeNotificadoEm?: string;
}

export interface OciosidadeResultado {
  emailDesabilitado: boolean;
  notificados: string[];
  desativados: string[];
}

export interface SimularOciosidadeRequest {
  mesesInativos?: number;
  notificadoHaDias?: number;
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
  idPerfil?: number;
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

export type AuditoriaAcao =
  | "CRIAR"
  | "ALTERAR"
  | "SUBSTITUIR"
  | "EXCLUIR"
  | "PROCESSAR";

export interface AuditoriaResponse {
  id: number;
  usuario: string;
  acao: AuditoriaAcao | string;
  entidade: string;
  entidadeId?: number;
  detalhes?: string;
  dataHora: string;
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
