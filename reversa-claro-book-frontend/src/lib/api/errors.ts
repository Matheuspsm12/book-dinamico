// Mapa chave-do-backend (i18n key em errors.properties) → mensagem amigável pt-BR.
// Tudo que não estiver no mapa cai no fallback.

const FRIENDLY: Record<string, string> = {
  // Auth
  "erro-credenciais-invalidas": "E-mail ou senha incorretos.",
  "erro-conta-pendente": "Sua conta está aguardando aprovação do administrador.",
  "erro-conta-rejeitada": "Seu cadastro foi rejeitado. Entre em contato com o administrador.",
  "erro-conta-desativada": "Sua conta foi desativada. Entre em contato com o administrador.",

  // Cadastro
  "erro-email-duplicado": "Já existe um usuário cadastrado com este e-mail.",

  // Aprovação / management
  "erro-decisao-invalida-status-nao-pendente":
    "Esta ação só é permitida em usuários com status PENDENTE.",
  "erro-usuario-ja-desativado": "Usuário já está desativado.",
  "erro-usuario-nao-desativado": "Usuário não está desativado.",
  "cap-usuarios-excedido":
    "Limite de 40 usuários aprovados atingido. Desative alguém antes de aprovar outro.",
  "email-desabilitado":
    "O envio de e-mail está desabilitado no servidor. Peça ao administrador para configurar SMTP ou resetar sua senha manualmente.",

  // Upload
  "arquivo-invalido": "Arquivo inválido. Verifique extensão, tamanho e conteúdo.",
  "arquivo-extensao-nao-permitida":
    "Extensão de arquivo não permitida. Aceitos: .xlsm, .xlsx, .pptx.",
  "arquivo-tamanho-excedido": "O arquivo excede o limite de 60 MB.",
  "arquivo-conteudo-incompativel":
    "O conteúdo do arquivo não corresponde à extensão informada.",
  "erro-lote-quantidades-divergentes":
    "Quantidade de metadados precisa bater com a quantidade de arquivos.",

  // Genérico
  "erro-inesperado": "Ocorreu um erro inesperado. Tente novamente em instantes.",
  "erro-validacao": "Dados inválidos. Verifique os campos.",
};

export function friendlyMessage(rawMessage: string | undefined | null): string {
  if (!rawMessage) return "Erro desconhecido.";
  const mapped = FRIENDLY[rawMessage];
  if (mapped) return mapped;
  // Mensagens de validação do Spring vêm formatadas tipo "campo: mensagem | outro: ..."
  // — exibimos como vieram.
  return rawMessage;
}
