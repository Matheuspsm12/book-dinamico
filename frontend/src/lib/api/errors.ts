const FRIENDLY: Record<string, string> = {
  "erro-credenciais-invalidas": "E-mail ou senha incorretos.",
  "erro-conta-pendente":
    "Sua conta está aguardando aprovação do administrador.",
  "erro-conta-rejeitada":
    "Seu cadastro foi rejeitado. Entre em contato com o administrador.",
  "erro-conta-desativada":
    "Sua conta foi desativada. Entre em contato com o administrador.",
  "erro-muitas-tentativas-tente-mais-tarde":
    "Muitas tentativas de login. Aguarde alguns minutos e tente novamente.",

  "erro-email-duplicado": "Já existe um usuário cadastrado com este e-mail.",

  "token-invalido":
    "Link de redefinição inválido ou expirado. Solicite um novo e-mail de recuperação.",

  "erro-decisao-invalida-status-nao-pendente":
    "Esta ação só é permitida em usuários com status PENDENTE.",
  "erro-usuario-ja-desativado": "Usuário já está desativado.",
  "erro-usuario-nao-desativado": "Usuário não está desativado.",
  "erro-ociosidade-usuario-nao-aprovado":
    "Só é possível simular ociosidade em usuários aprovados.",
  "erro-simulacao-indisponivel-em-producao":
    "A simulação de ociosidade não está disponível em produção.",
  "erro-usuario-base-protegido":
    "Este é um usuário base do sistema e não pode ser gerenciado.",
  "cap-usuarios-excedido":
    "Limite de 40 usuários aprovados atingido. Desative alguém antes de aprovar outro.",
  "email-desabilitado":
    "O envio de e-mail está desabilitado no servidor. Peça ao administrador para configurar SMTP ou resetar sua senha manualmente.",

  "arquivo-invalido":
    "Arquivo inválido. Verifique extensão, tamanho e conteúdo.",
  "arquivo-extensao-nao-permitida":
    "Extensão de arquivo não permitida. Aceitos: .xlsx, .xlsm, .xlsb, .xltx, .xltm, .pptx.",
  "arquivo-tamanho-excedido": "O arquivo excede o limite de 60 MB.",
  "arquivo-conteudo-incompativel":
    "O conteúdo do arquivo não corresponde à extensão informada.",
  "erro-lote-quantidades-divergentes":
    "Quantidade de metadados precisa bater com a quantidade de arquivos.",
  "erro-limite-armazenamento":
    "Limite de 2 GB de armazenamento atingido. Exclua documentos para liberar espaço antes de enviar novos arquivos.",

  "erro-inesperado":
    "Ocorreu um erro inesperado. Tente novamente em instantes.",
  "erro-validacao": "Dados inválidos. Verifique os campos.",
  "erro-data-invalida":
    "Data de atualização inválida. Use o formato dd/mm/aaaa.",
  "arquivo-obrigatorio": "Envie o arquivo para concluir a operação.",
  "parametro-obrigatorio": "Faltam dados obrigatórios na requisição.",
  "content-type-nao-suportado": "Tipo de conteúdo da requisição não suportado.",
};

export function friendlyMessage(rawMessage: string | undefined | null): string {
  if (!rawMessage) return "Erro desconhecido.";
  const mapped = FRIENDLY[rawMessage];
  if (mapped) return mapped;
  return rawMessage;
}
