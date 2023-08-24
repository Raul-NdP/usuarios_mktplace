package br.com.senai.usuariosmktplace.core.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import br.com.senai.usuariosmktplace.core.dao.DaoUsuario;
import br.com.senai.usuariosmktplace.core.dao.FactoryDao;
import br.com.senai.usuariosmktplace.core.domain.Usuario;

public class UsuarioService {
	
	private DaoUsuario dao;

	public UsuarioService() {
		dao = FactoryDao.getInstance().getDaoUsuario();
	}
	
	public Usuario criarPor(String nomeCompleto, String senha) { 
		
		this.validar(nomeCompleto, senha);
		String login = gerarLoginPor(nomeCompleto);
		String senhaCriptografada = gerarHashDa(senha);
		
		Usuario usuario = new Usuario(login, senhaCriptografada, nomeCompleto);
		
		this.dao.inserir(usuario);
		
		Usuario usuarioSalvo = dao.buscarPor(login);
		
		return usuarioSalvo;
		
	}
	
	@SuppressWarnings("deprecation")
	private void validar(String senha) {
		
		boolean isSenhaValida = !Strings.isNullOrEmpty(senha)
				&& senha.length() >= 6
				&& senha.length() <= 15;
		
		Preconditions.checkArgument(isSenhaValida, "A senha é obrigatória "
				+ "e deve possuir entre 6 e 15 caracteres");
		
		boolean isContemLetra = CharMatcher.inRange('a','z').countIn(senha.toLowerCase()) > 0;
		boolean isContemNumero = CharMatcher.inRange('0','9').countIn(senha) > 0;
		boolean isCaracterInvalido = !CharMatcher.javaLetterOrDigit().matchesAllOf(senha);
		
		Preconditions.checkArgument(!isCaracterInvalido && isContemLetra && isContemNumero, 
				"A senha deve possuir somente e obrigatoriamente letras e números");
		
	}
	
	private void validar(String nomeCompleto, String senha) {
		
		List<String> partesNome = fracionar(nomeCompleto);
		
		boolean isNomeCompleto = partesNome.size() > 1;
		
		boolean isNomeValido = !Strings.isNullOrEmpty(nomeCompleto) 
				&& isNomeCompleto
				&& nomeCompleto.length() >= 5 && nomeCompleto.length() <= 120;
				
		Preconditions.checkArgument(isNomeValido, "o nome é obrigatório, deve possuir entre 5 e 120 caracteres e deve possuir sobrenome também");
		
		this.validar(senha);
		
	}
	
	private String removerAcentoDo(String nomeCompleto) {
		
		return Normalizer.normalize(nomeCompleto, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
		
	}
	
	private List<String> fracionar(String nomeCompleto) {
		
		List<String> nomeFracionado = new ArrayList<String>();
		
		if (nomeCompleto != null && !nomeCompleto.isBlank()) {
			
			String[] partesNome = nomeCompleto.split(" ");
			
			for (String parte : partesNome) {
				boolean isNaoContemArtigo = !parte.equalsIgnoreCase("e")
						&& !parte.equalsIgnoreCase("de")
						&& !parte.equalsIgnoreCase("da")
						&& !parte.equalsIgnoreCase("dos")
						&& !parte.equalsIgnoreCase("das");
				
				if (isNaoContemArtigo) {
					nomeFracionado.add(parte.toLowerCase());
				}
			}
		}
		return nomeFracionado;
	}
	
	private String gerarLoginPor(String nomeCompleto) {
		
		nomeCompleto = this.removerAcentoDo(nomeCompleto);
		List<String> partesNome = this.fracionar(nomeCompleto);
		
		String loginGerado = null;
		Usuario usuarioEncontrado = null;
		
		if (!partesNome.isEmpty()) {
			for (int i = 1; i < partesNome.size(); i++) {
				loginGerado = partesNome.get(0) + "." + partesNome.get(i);
				usuarioEncontrado = dao.buscarPor(loginGerado);
				if (usuarioEncontrado == null) {
					return loginGerado;
				}
			}
			
			int proximoSequencial = 0;
			String loginDisponivel = null;
			
			while (usuarioEncontrado != null) {
				loginDisponivel = loginGerado + ++ proximoSequencial;
				usuarioEncontrado = dao.buscarPor(loginDisponivel);
			}
			
			loginGerado = loginDisponivel;
		}
		return loginGerado;
	}
	
	private String gerarHashDa(String senha) {
		
		return new DigestUtils(MessageDigestAlgorithms.SHA3_256).digestAsHex(senha);
		
	}
	
}