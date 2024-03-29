/**
 * 
 */
package br.gov.ce.fortaleza.cti.sgf.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Query;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.gov.ce.fortaleza.cti.sgf.entity.CotaKm;
import br.gov.ce.fortaleza.cti.sgf.entity.UG;
import br.gov.ce.fortaleza.cti.sgf.entity.Veiculo;
import br.gov.ce.fortaleza.cti.sgf.util.StatusVeiculo;

/**
 * @author deivid
 *
 */
@Repository
@Transactional
public class CotaKmService extends BaseService<Integer, CotaKm>{

	@SuppressWarnings("unchecked")
	public List<CotaKm> pesquisar(CotaKm cota) {
		List<CotaKm> cotas = null;
		Session session = (Session) entityManager.getDelegate();
		Criteria criteria = session.createCriteria(CotaKm.class);
		if(cota.getVeiculo() != null){
			criteria.createCriteria("veiculo").add(Example.create(cota.getVeiculo()).enableLike(MatchMode.ANYWHERE).ignoreCase());
			if(cota.getVeiculo().getModelo() != null){
				criteria.createCriteria("veiculo.modelo").add(Example.create(cota.getVeiculo().getModelo()).enableLike(MatchMode.ANYWHERE).ignoreCase());
				if(cota.getVeiculo().getModelo().getMarca() != null){
					criteria.createCriteria("veiculo.modelo.marca").add(Example.create(cota.getVeiculo().getModelo().getMarca()).enableLike(MatchMode.ANYWHERE).ignoreCase());
				}
			}
			if(cota.getVeiculo().getUa() != null){
				criteria.createCriteria("veiculo.ua.ug").add(Example.create(cota.getVeiculo().getUa().getUg()));
			}
		}
		cotas = criteria.list();
		List<CotaKm> remove = new ArrayList<CotaKm>();
		for (CotaKm c : cotas) {
			if(c.getVeiculo().getStatus().equals(StatusVeiculo.baixado)){
				remove.add(c);
			}
		}
		cotas.removeAll(remove);
		remove = new ArrayList<CotaKm>();
		return cotas;
	}

	//MODIFICADO 02.06.2014 - PAULO ANDRE
	public Collection<? extends CotaKm> findcotasPaginaInicial() {
		Query query = entityManager.createQuery("SELECT c FROM CotaKm c WHERE c.veiculo.status <> ?");
		query.setParameter(1, StatusVeiculo.baixado);
		query.setFirstResult(1);
		query.setMaxResults(30);
		List<CotaKm> result = new ArrayList<CotaKm>(query.getResultList());
		//PREENCHE OS DADOS DE CADA VEICULO DA LISTA
		//DXA A PESQUISA MAIS LENTA
		for (CotaKm cotaKm : result) {
			Hibernate.initialize(cotaKm.getVeiculo());
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public Collection<? extends CotaKm> findcotasKmAllVeiculoativos2(UG ug) {
		Query query;		
		if(ug != null){
			query = entityManager.createQuery("SELECT c FROM CotaKm c WHERE c.veiculo.status <> ? and c.veiculo.ua.ug = ?");
			query.setParameter(1, StatusVeiculo.baixado);
			query.setParameter(2, ug);
		}else {
			query = entityManager.createQuery("SELECT c FROM CotaKm c WHERE c.veiculo.status <> ?");
			query.setParameter(1, StatusVeiculo.baixado);
		}
		List<CotaKm> result = new ArrayList<CotaKm>(query.getResultList());
		//PREENCHE OS DADOS DE CADA VEICULO DA LISTA
		for (CotaKm cotaKm : result) {
			Hibernate.initialize(cotaKm.getVeiculo());
		}
		return result;
	}
	//FIM
	
	@SuppressWarnings("unchecked")
	public Collection<? extends CotaKm> findcotasKmAllVeiculoativos() {
		// TODO Auto-generated method stub
		Query query = entityManager.createQuery("SELECT c FROM CotaKm c WHERE c.veiculo.status <> ?");
		query.setParameter(1, StatusVeiculo.baixado);
		List<CotaKm> result = new ArrayList<CotaKm>(query.getResultList());
		
		return result;
	}

	public Object findByPlacaVeiculo(String placa) {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("unchecked")
	public Collection<? extends Veiculo> findVeiculosTerceiros() {
		// MODIFICADO 04.06.2014 - PAULO ANDRE
		//Query query = entityManager.createQuery("select o from Veiculo o where o.propriedade = 'Locado' and o not in(select c.veiculo from CotaKm c) and o.status != 6");
		Query query = entityManager.createQuery("select o from Veiculo o where o.abastecimento = 0 and o not in(select c.veiculo from CotaKm c) and o.status != 6");
		List<Veiculo> veiculos = new ArrayList<Veiculo>(query.getResultList());
		return veiculos;
	}
}
