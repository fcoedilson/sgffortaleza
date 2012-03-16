/**
 * 
 */
package br.gov.ce.fortaleza.cti.sgf.bean;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import br.gov.ce.fortaleza.cti.sgf.entity.Bomba;
import br.gov.ce.fortaleza.cti.sgf.entity.DiarioBomba;
import br.gov.ce.fortaleza.cti.sgf.entity.Posto;
import br.gov.ce.fortaleza.cti.sgf.entity.User;
import br.gov.ce.fortaleza.cti.sgf.service.BombaService;
import br.gov.ce.fortaleza.cti.sgf.service.DiarioBombaService;
import br.gov.ce.fortaleza.cti.sgf.service.PostoService;
import br.gov.ce.fortaleza.cti.sgf.util.DateUtil;
import br.gov.ce.fortaleza.cti.sgf.util.JSFUtil;
import br.gov.ce.fortaleza.cti.sgf.util.SgfUtil;
import br.gov.ce.fortaleza.cti.sgf.util.dto.RelatorioDTO;


@Scope("session")
@Component("diarioBombaBean")
public class DiarioBombaBean extends EntityBean<Integer, DiarioBomba>{

	@Autowired
	private DiarioBombaService service;

	@Autowired
	private PostoService postoService;

	@Autowired
	private BombaService bombaService;

	private List<Posto> postos;
	private Bomba bombaSelecionada;
	private Boolean bombaAberta;
	private Boolean bombaFechada;
	private Boolean start;
	private User user = SgfUtil.usuarioLogado();
	private Boolean mostrarZeraBomba = false;

	private Date init;
	private Date finnal;
	private Posto posto;

	@Override
	protected Integer retrieveEntityId(DiarioBomba entity) {
		return entity.getId();
	}

	@Override
	protected DiarioBombaService retrieveEntityService() {
		this.bombaSelecionada = new Bomba();
		return this.service;
	}

	protected DiarioBomba createNewEntity() {
		DiarioBomba diario = new DiarioBomba();
		return diario;
	}

	/**
	 * Verifica o estado atual da diária e chama o método prepareUpdate ou prepareSave
	 * @return
	 */
	public String prepareState(){
		String retorno = null;
		this.entity.setZerada(false);
		if(this.entity.getStatus() == 0){
			retorno = this.prepareUpdate();
		} else if(this.entity.getStatus() == 1 && this.entity.getValorFinal() == null){
			retorno = this.prepareSave();
		} else if(this.entity.getStatus() == 1 && this.entity.getValorFinal() != null){
			JSFUtil.getInstance().addErrorMessage("msg.error.bomba.fechada");
		}
		return retorno;
	}

	/**
	 * validação do diário de bomba de combustível
	 * @return
	 */

	public Boolean validaLeitura(){
		/* se o estado não for EDIT */
		if (!isEditState()) {
			if(this.entity.getValorFinal() == null){
				JSFUtil.getInstance().addErrorMessage("msg.error.bomba.valFim");
				return false;
			}
		}
		/* 
		 * se já foi informado a leitura inicial e leitura final 
		 * 
		 */
		if(this.entity.getValorInicial() != null && this.entity.getValorFinal() != null){

			/* 
			 * Caso a leitura final informada seja menor que leitura inicial, retorna erro
			 * Verifica flag pra saber se a bomba zerou na leitura anterior
			 */
			if((this.entity.getValorFinal() < this.entity.getValorInicial()) && !this.entity.isZerada()){
				JSFUtil.getInstance().addErrorMessage("msg.error.bomba.valFim.inconsistente");
				return false;
			} else 
				/*
				 * 	
				 */
				if(this.entity.getValorInicial() > this.entity.getBomba().getLimiteLeitura()){
					JSFUtil.getInstance().addErrorMessage("msg.error.bomba.valorNaoPermitido");
					return false;
				} else {
					return true;
				}
		} else {
			return true;
		}
	}

	@Override
	public String prepareSave() {
		this.mostrarZeraBomba = false;
		DiarioBomba ultimaDiaria = service.findCurrentDiaryByBomba(this.entity.getBomba().getId());
		this.entity.setHoraInicial(new Date());
		if(ultimaDiaria != null){
			this.entity.setValorInicial(ultimaDiaria.getValorFinal());
			if(this.entity.getValorInicial() != null){
				verificaBombaZerada();
			} else {
				JSFUtil.getInstance().addErrorMessage("msg.error.bomba.valorNaoPermitido");
			}
		}
		setCurrentBean(currentBeanName());
		setCurrentState(SAVE);
		return SUCCESS;
	}

	@Override
	public String prepareUpdate() {
		this.mostrarZeraBomba = false;
		if(this.entity.getValorInicial() != null){
			verificaBombaZerada();
		}
		return super.prepareUpdate();
	}

	@Override
	public String save() {
		DiarioBomba ultimaDiaria = service.findCurrentDiaryByBomba(this.entity.getBomba().getId());
		if(this.entity.getValorInicial() == null){
			JSFUtil.getInstance().addErrorMessage("msg.error.bomba.valorInicialNulo");
			return FAIL;
		} else if(ultimaDiaria != null && ultimaDiaria.getValorFinal() != null && (ultimaDiaria.getValorFinal() > this.entity.getValorInicial())){
			JSFUtil.getInstance().addErrorMessage("msg.error.bomba.valorInicialMenorQFinalAnterior");
			return FAIL;
		} if(this.entity.getValorFinal() != null){
			if(this.entity.getValorFinal() >  (this.entity.getBomba().getLimiteLeitura()-100)){
				JSFUtil.getInstance().addErrorMessage("msg.error.bomba.valorNaoPermitido");
				return FAIL;
			}
		} if (this.entity.getValorInicial() != null){
			if(this.entity.getValorInicial() > (this.entity.getBomba().getLimiteLeitura() - 100)){
				JSFUtil.getInstance().addErrorMessage("msg.error.bomba.valorNaoPermitido");
				return FAIL;
			}
		}

		this.entity.setStatus(0);
		this.entity.setImageStatus("/images/open_icon.png");
		this.entity.setHoraInicial(new Date());
		this.entity.setDataCriacao(new Date());
		this.entity.setUsuarioCricao(SgfUtil.usuarioLogado().getCodPessoaUsuario());
		return super.save();
	}

	@Override
	public String update() {
		if(validaLeitura()){
			if(this.entity.getValorInicial() == null){
				this.entity.setValorFinal(null);
				this.entity.setHoraFinal(null);
				this.entity.setHoraInicial(null);
				this.entity.setStatus(1);
				this.entity.setImageStatus("/images/closed.gif");
				service.delete(this.entity.getId());
			} else if(this.entity.getValorFinal() == null){
				this.entity.setStatus(0);
				this.entity.setImageStatus("/images/open_icon.png");
				this.entity.setHoraFinal(null);
				super.update();
			} else {
				this.entity.setStatus(1);
				this.entity.setImageStatus("/images/tick.png");
				this.entity.setHoraFinal(new Date());
				super.update();
			}
			return searchSort();
		} else {
			return FAIL;
		}
	}
	/**
	 * atualiza diário de bomba
	 * @return
	 */
	public String atualizarDiarioBomba(){
		if(validaLeitura()){
			this.entity.setUltimaAlteracao(new Date());
			this.entity.setUsuarioAlteracao(SgfUtil.usuarioLogado().getCodPessoaUsuario());
			retrieveEntityService().update(this.entity);
			setCurrentState(RelatorioDTO.SEARCH_DIARIOBOMBA);
			setCurrentBean(currentBeanName());
			return SUCCESS;
		} else {
			return FAIL;
		}
	}

	@Override
	public String searchSort() {
		iniciarDiarias();
		setCurrentBean(currentBeanName());
		setCurrentState(SEARCH);
		this.interval = 2000000;
		return SUCCESS;
	}

	/**
	 * atualiza flag ZERADA para TRUE
	 */
	public void zerarBomba(){
		this.entity.setZerada(true);
	}

	/**
	 * this.service.findDiariasByBomba(bombaSelecionada) faz uma consulta a todos as di�rias
	 * @return
	 */
	private Boolean existeDiarias() {
		Date ultimaDiaria = service.findUltimaDiariaByBomba(bombaSelecionada.getId());
		Calendar calendar = Calendar.getInstance();
		Calendar calendar2 = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar2.setTime(ultimaDiaria);
		if(calendar2.get(Calendar.DAY_OF_MONTH) == calendar.get(Calendar.DAY_OF_MONTH)){
			return true;
		}
		return false;
	}

	public void verificaBombaZerada(){
		if(!this.entity.getBomba().getLimiteLeitura().equals(null)){
			if(this.entity.getValorInicial() > this.entity.getBomba().getLimiteLeitura() || 
					this.entity.getValorFinal() > this.entity.getBomba().getLimiteLeitura()){
				setMostrarZeraBomba(true);
			} else {
				setMostrarZeraBomba(false);
			}
		} else {
			JSFUtil.getInstance().addErrorMessage("msg.error.bomba.semvalor.limiteleitura");
		}
	}

	public String voltar(){
		this.mostrarZeraBomba = false;
		iniciarDiarias();
		return super.searchSort();
	}

	/**
	 * Iniciar as di�rias da bombas de cada posto
	 */
	public void iniciarDiarias() {

		this.postos = new ArrayList<Posto>();
		List<Posto> listPostos = new ArrayList<Posto>();

		if(SgfUtil.isAdministrador(this.user) || SgfUtil.isCoordenador(this.user)){
			listPostos = postoService.retrieveAll();
		} else if(SgfUtil.isOperador(this.user)){
			listPostos.add(postoService.retrieve(this.user.getPosto().getCodPosto()));
		}

		for (Posto posto : listPostos) {
			for (Bomba bomba : posto.getListaBomba()) {
				boolean existeDiaria = false;
				Calendar calendar = Calendar.getInstance();
				Calendar calendar2 = Calendar.getInstance();
				calendar.setTime(new Date()); // dia atual
				Date ultimaDiaria = service.findUltimaDiariaByBomba(bomba.getId());
				DiarioBomba diaria = service.findCurrentDiaryByBomba(bomba.getId());

				if(ultimaDiaria != null){
					calendar2.setTime(ultimaDiaria);
					if( (calendar2.get(Calendar.DAY_OF_MONTH) < calendar.get(Calendar.DAY_OF_MONTH)) || 
							(calendar2.get(Calendar.MONTH) < calendar.get(Calendar.MONTH)) || (calendar2.get(Calendar.YEAR) < calendar.get(Calendar.YEAR))){
						existeDiaria = false;
					} else {
						existeDiaria = true;
					}
				}
				if(existeDiaria){
					diaria.setImageStatus("/images/tick.png");
					bomba.setDiarioBomba(diaria);
				} else {
					DiarioBomba db = new DiarioBomba();
					db.setDataCadastro(new Date());
					db.setHoraInicial(null);
					db.setHoraFinal(null);
					db.setStatus(1);
					db.setImageStatus("/images/closed.gif");
					db.setUser(user);
					db.setValorInicial(null);
					db.setBomba(bomba);
					bomba.setDiarioBomba(db);
				}
			}
			this.postos.add(posto);
		}
	}

	/**
	 * Caso a bomba n�o tenha nenhuma di�ria, uma di�ria ser� criada
	 * Caso Contr�rio a bomba tenha di�ria, a di�rio corrente ser� mostrada
	 * @param list
	 */
	public void setarValores(List<Posto> list) {
		for (Posto posto : list) {
			for (Bomba bomba : posto.getListaBomba()) {
				if(bomba.getDiarioBombas() == null || bomba.getDiarioBombas().isEmpty()){
					DiarioBomba d = new DiarioBomba();
					d.setDataCadastro(new Date());
					d.setHoraInicial(null);
					d.setHoraFinal(null);
					d.setStatus(1);
					d.setImageStatus("/images/closed.gif");
					d.setUser(user);
					d.setValorInicial(null);
					d.setBomba(bomba);
					bomba.setDiarioBomba(d);
				} else {
					boolean diariaHoje = false;
					for (DiarioBomba diarioBomba : bomba.getDiarioBombas()) {
						if(DateUtil.compareDate(DateUtil.getDateStartDay(new Date()), DateUtil.getDateStartDay(diarioBomba.getDataCadastro()))){
							diariaHoje = true;
							if(diarioBomba.getStatus() == 1){
								diarioBomba.setImageStatus("/images/tick.png");
							} else if(diarioBomba.getStatus() == 0){
								diarioBomba.setImageStatus("/images/open_icon.png");
							}
							bomba.setDiarioBomba(diarioBomba);
							break;
						}
					}
					if(!diariaHoje){
						DiarioBomba d = new DiarioBomba();
						d.setDataCadastro(new Date());
						d.setHoraInicial(null);
						d.setHoraFinal(null);
						d.setStatus(1);
						d.setImageStatus("/images/closed.gif");
						d.setUser(user);
						d.setBomba(bomba);
						bomba.setDiarioBomba(d);
					}	
				}
			}
		}
	}

	public String searchDiarioBomba() {

		if(this.init != null && this.finnal != null){
			this.init = DateUtil.getDateStartDay(this.init);
			this.finnal = DateUtil.getDateEndDay(this.finnal);
			this.entities = service.ultimasTrintaDiarias(init, finnal, null);
		} else {
			this.entities = service.ultimasTrintaDiarias();
		}
		setCurrentState(RelatorioDTO.SEARCH_DIARIOBOMBA);
		setCurrentBean(currentBeanName());
		return SUCCESS;
	}


	public String listarDiariosBomba(){
		this.init = DateUtil.getDateStartDay(this.init);
		this.finnal = DateUtil.getDateEndDay(this.finnal);
		if(this.init != null && this.finnal != null && this.posto != null){
			this.entities = service.ultimasTrintaDiarias(init, finnal, posto);
		}
		return SUCCESS;
	}

	public String updateDiarioBomba() {
		setCurrentState(RelatorioDTO.UPDATE_DIARIOBOMBA);
		setCurrentBean(currentBeanName());
		return SUCCESS;
	}

	public boolean isSearchDiarioBombaState() {
		return RelatorioDTO.SEARCH_DIARIOBOMBA.equals(getCurrentState());
	}

	public boolean isUpdateDiarioBombaState() {
		return RelatorioDTO.UPDATE_DIARIOBOMBA.equals(getCurrentState());
	}

	public Float recuperarValorFinal(DiarioBomba diarioBomba, Bomba bomba) {
		DiarioBomba diarioBombaAux = this.service.findLastDiariaByDate(bomba);
		if(diarioBomba.getDataCadastro().equals(diarioBombaAux.getDataCadastro())){
			return diarioBomba.getValorFinal();
		} else {
			return diarioBombaAux.getValorFinal();
		}
	}

	public Date getInit() {
		return init;
	}

	public void setInit(Date init) {
		this.init = init;
	}

	public Date getFinnal() {
		return finnal;
	}

	public void setFinnal(Date finnal) {
		this.finnal = finnal;
	}

	public Posto getPosto() {
		return posto;
	}

	public void setPosto(Posto posto) {
		this.posto = posto;
	}

	public void setBombaSelecionada(Bomba bombaSelecionada) {
		this.bombaSelecionada = bombaSelecionada;
	}

	public Bomba getBombaSelecionada() {
		return bombaSelecionada;
	}

	public Boolean getBombaAberta() {
		return bombaAberta;
	}

	public void setBombaAberta(Boolean bombaAberta) {
		this.bombaAberta = bombaAberta;
	}

	public Boolean getBombaFechada() {
		return bombaFechada;
	}

	public void setBombaFechada(Boolean bombaFechada) {
		this.bombaFechada = bombaFechada;
	}

	public boolean isStart(){
		return start;
	}

	public void setStart(Boolean start) {
		this.start = start;
	}

	public void setMostrarZeraBomba(Boolean mostrarZeraBomba) {
		this.mostrarZeraBomba = mostrarZeraBomba;
	}

	public Boolean getMostrarZeraBomba() {
		return mostrarZeraBomba;
	}

	public List<Posto> getPostos() {
		return postos;
	}

	public void setPostos(List<Posto> postos) {
		this.postos = postos;
	}
}