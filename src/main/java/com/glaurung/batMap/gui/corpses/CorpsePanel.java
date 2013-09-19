package com.glaurung.batMap.gui.corpses;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import com.glaurung.batMap.controller.MapperPlugin;
import com.glaurung.batMap.io.CorpseHandlerDataPersister;

public class CorpsePanel extends JPanel implements ActionListener, ComponentListener{

	private CorpseModel model = new CorpseModel();
	private String BASEDIR;
	private MapperPlugin plugin;
	private final Color TEXT_COLOR = Color.LIGHT_GRAY;
	private final Color BG_COLOR = Color.BLACK;
	private final int BORDERLINE=7;
	private final int TOP_BORDER=20;
	private Font font = new Font("Consolas",Font.PLAIN,14);
	private final int CB_WIDTH=200;
	private final int BUTTON_WIDTH=70;
	private final int LABEL_WIDTH=100;
	private final int CB_HEIGHT=25;
	
	

	public CorpsePanel(String BASEDIR, MapperPlugin plugin) {
		this.BASEDIR=BASEDIR;
		this.model = CorpseHandlerDataPersister.load(BASEDIR);
		if(model == null){
			model = new CorpseModel();
		}else{
			loadFromModel();
		}
		this.plugin = plugin;
		this.setPreferredSize(new Dimension(1200, 800));
		this.redoLayout();
		
		this.delim.addActionListener(this);
		//TODO: add listener for  lootlists to update the effects on the checkboxes that rely on them
		//TODO: also make lootlists editable and scrollable etc
		
		this.mount.addActionListener(this);
		this.setBackground(BG_COLOR);
		on.addActionListener(this);
		off.addActionListener(this);
		clear.addActionListener(this);
	}

	private CorpseCheckBox lichdrain = 			new CorpseCheckBox("lich drain soul",false,"lich drain",this, font);
	private CorpseCheckBox kharimsoul = 		new CorpseCheckBox("kharim drain soul",false,"kharim drain",this, font);
	private CorpseCheckBox kharimSoulCorpse=	new CorpseCheckBox("kharim dest corpse",false,null,this, font);
	private CorpseCheckBox tsaraksoul = 		new CorpseCheckBox("tzarak drain soul",false,"tzarak drain soul",this, font);
	private CorpseCheckBox ripSoulToKatana=		new CorpseCheckBox("shitKatana rip soul",false,"rip soul from corpse",this, font);
	private CorpseCheckBox arkemile = 			new CorpseCheckBox("necrostaff arkemile",false,"say arkemile",this, font);
	private CorpseCheckBox gac = 				new CorpseCheckBox("get all from corpse",false,"get all from corpse",this, font);
	private CorpseCheckBox ga = 				new CorpseCheckBox("get all",false,"get all",this, font);
	private CorpseCheckBox eatCorpse = 			new CorpseCheckBox("get and eat corpse",false,"get corpse"+getDelim()+"eat corpse",this, font);
	private CorpseCheckBox donate = 			new CorpseCheckBox("donate noeq and drop rest",false,"get all from corpse"+getDelim()+"donate noeq"+getDelim()+"drop noeq",this, font);
	private CorpseCheckBox lootCorpse = 		new CorpseCheckBox("get loot from corpse",false,"get "+getLootString()+" from corpse",this, font);
	private CorpseCheckBox lootGround = 		new CorpseCheckBox("get loot from ground",false,"get "+getLootString(),this, font);
	private CorpseCheckBox barbarianBurn = 		new CorpseCheckBox("barbarian burn corpse",false,"barbburn",this, font);
	private CorpseCheckBox feedCorpseTo = 		new CorpseCheckBox("feed corpse to mount",false,"get corpse"+getDelim()+"feed corpse to "+getMountName(),this, font);
	private CorpseCheckBox beheading = 			new CorpseCheckBox("kharim behead corpse",false,"use beheading of departed",this, font);
	private CorpseCheckBox desecrateGround=		new CorpseCheckBox("desecrate ground",false,"use desecrate ground",this, font);
	private CorpseCheckBox burialCere=			new CorpseCheckBox("burial ceremony",false,"use burial ceremony",this, font);
	private CorpseCheckBox dig = 				new CorpseCheckBox("dig grave",false,"dig grave",this, font);
	private CorpseCheckBox wakeFollow=			new CorpseCheckBox("follow",false," follow",this, font);
	private CorpseCheckBox wakeAgro=			new CorpseCheckBox("agro",false," agro",this, font);
	private CorpseCheckBox wakeTalk=			new CorpseCheckBox("talk",false," talk",this, font);
	private CorpseCheckBox wakeStatic=			new CorpseCheckBox("static",false," static",this, font);
	private CorpseCheckBox lichWake = 			new CorpseCheckBox("lich wake corpse",false,"lick wake corpse",this, font);
	private CorpseCheckBox vampireWake=			new CorpseCheckBox("vampire wake corpse",false,"vampire wake corpse",this, font);
	private CorpseCheckBox skeletonWake=		new CorpseCheckBox("skeleton wake corpse",false,"skeleton wake corpse",this, font);
	private CorpseCheckBox zombieWake=			new CorpseCheckBox("zombie wake corpse",false,"zombie wake corpse",this, font);
	private CorpseCheckBox aelenaOrgan=			new CorpseCheckBox("aelena extract organ",false,"familiar harvest ",this, font);
	private CorpseCheckBox aelenaFam=			new CorpseCheckBox("aelena fam consume corpse",false,"familiar consume corpse",this, font);
	
	
	private static final long serialVersionUID = 1L;
	private JCheckBox on = 	new JCheckBox("On!"); 
	private JCheckBox off =	new JCheckBox("Off");
	private JTextField delim = 	new JTextField("");
	private JTextField mount = new JTextField("");
	private JButton clear = 		new JButton("Clear!");
	private JList lootLists = 	new JList();
	private JTextField organ1 = new JTextField("");
	private JTextField organ2 = new JTextField("");
	
	
	private Border whiteline = BorderFactory.createLineBorder(Color.white);
	private JPanel soulPanel =new JPanel();
	private JPanel listPanel =new JPanel();
	private JPanel controlPanel =new JPanel();
	private JPanel wakePanel =new JPanel();
	private JPanel lootPanel =new JPanel();
	private JPanel corpsePanel = new JPanel();
	private JLabel delimLabel= new JLabel("delimeter:");
	private JLabel mountLabel= new JLabel("mount name:");
	private JLabel organ1Label= new JLabel("first organ:");
	private JLabel organ2Label= new JLabel("second organ:");
	
	
	//new JPanel(BorderFactory.createTitledBorder(whiteline, "Souls"));
//	private JPane corpsePanel = new JP
	
	
	private String getDelim(){
		return model.getDelim();
	}
	

	private String getMountName() {
		return model.getMountHandle();
	}


	private String getLootString() {
		String loots = "";
		for (String lootItem : model.getLootList()){
			loots+=lootItem+",";
		}
		return loots.substring(0, loots.length());
	}

	private void saveToModel(){
		this.model.setMountHandle(mount.getText());
		this.model.setDelim(delim.getText());
		this.model.setLootList(createStringLootList());
		this.model.setOrgan1(organ1.getText());
		this.model.setOrgan2(organ2.getText());
		this.model.lichdrain = lichdrain.isSelected();
		this.model.kharimsoul = kharimsoul.isSelected();
		this.model.kharimSoulCorpse = kharimSoulCorpse.isSelected();
		this.model.tsaraksoul = tsaraksoul.isSelected();
		this.model.ripSoulToKatana=ripSoulToKatana.isSelected();
		this.model.arkemile = arkemile.isSelected();
		this.model.gac = gac.isSelected();
		this.model.ga =ga.isSelected();
		this.model.eatCorpse = eatCorpse.isSelected();
		this.model.donate = donate.isSelected();
		this.model.lootCorpse = lootCorpse.isSelected();
		this.model.lootGround = lootGround.isSelected();
		this.model.barbarianBurn = barbarianBurn.isSelected();
		this.model.feedCorpseTo = feedCorpseTo.isSelected();
		this.model.beheading = beheading.isSelected();
		this.model.desecrateGround=desecrateGround.isSelected();
		this.model.burialCere=burialCere.isSelected();
		this.model.dig = dig.isSelected();
		this.model.wakeFollow=wakeFollow.isSelected();
		this.model.wakeAgro=wakeAgro.isSelected();
		this.model.wakeTalk=wakeTalk.isSelected();
		this.model.wakeStatic=wakeStatic.isSelected();
		this.model.lichWake = lichWake.isSelected();
		this.model.vampireWake=vampireWake.isSelected();
		this.model.skeletonWake=skeletonWake.isSelected();
		this.model.zombieWake=zombieWake.isSelected();
		this.model.aelenaFam = aelenaFam.isSelected();
		this.model.aelenaOrgan = aelenaOrgan.isSelected();
		CorpseHandlerDataPersister.save(BASEDIR, this.model);
		
	}
	private List<String> createStringLootList() {
		LinkedList<String> list = new LinkedList<String>();
		ListModel listModel = lootLists.getModel();
		for (int i=0;i<listModel.getSize();++i){
			list.add((String) listModel.getElementAt(i));
		}
		return list;
	}

	private void loadFromModel(){
		
		
		lichdrain.setSelected(this.model.lichdrain);
		kharimsoul.setSelected(this.model.kharimsoul);
		kharimSoulCorpse.setSelected(this.model.kharimSoulCorpse);
		tsaraksoul.setSelected(this.model.tsaraksoul);
		ripSoulToKatana.setSelected(this.model.ripSoulToKatana);
		arkemile.setSelected(this.model.arkemile);
		gac.setSelected(this.model.gac);
		ga.setSelected(this.model.ga);
		eatCorpse.setSelected(this.model.eatCorpse);
		donate.setSelected(this.model.donate);
		lootCorpse.setSelected(this.model.lootCorpse);
		lootGround.setSelected(this.model.lootGround);
		barbarianBurn.setSelected(this.model.barbarianBurn);
		feedCorpseTo.setSelected(this.model.feedCorpseTo);
		beheading.setSelected(this.model.beheading);
		desecrateGround.setSelected(this.model.desecrateGround);
		burialCere.setSelected(this.model.burialCere);
		dig.setSelected(this.model.dig);
		wakeFollow.setSelected(this.model.wakeFollow);
		wakeAgro.setSelected(this.model.wakeAgro);
		wakeTalk.setSelected(this.model.wakeTalk);
		wakeStatic.setSelected(this.model.wakeStatic);
		lichWake.setSelected(this.model.lichWake);
		vampireWake.setSelected(this.model.vampireWake);
		skeletonWake.setSelected(this.model.skeletonWake);
		zombieWake.setSelected(this.model.zombieWake);
		aelenaFam.setSelected(this.model.aelenaFam);
		aelenaOrgan.setSelected(this.model.aelenaOrgan);
		
		mount.setText(this.model.getMountHandle());
		delim.setText(this.model.getDelim());
		organ1.setText(this.model.getOrgan1());
		organ2.setText(this.model.getOrgan2());
		lootLists.setListData(this.model.getLootList().toArray());
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		
		//TODO: click through all and make sure everything is hooked up ( dig fails?)
			Object source = event.getSource();
			if(source == on){
				on.setSelected(true);
				off.setSelected(false);
				this.plugin.toggleRipAction(true);
			}else if(source == off){
				on.setSelected(false);
				off.setSelected(true);
				this.plugin.toggleRipAction(false);
			}else if (source == kharimSoulCorpse){
				this.plugin.doCommand("kharim set corpseDest foobar");
			}else if(source == lichdrain){
				turnOff(kharimsoul,kharimsoul,ripSoulToKatana,arkemile);
			}else if(source == kharimsoul){
				turnOff(lichdrain,tsaraksoul,ripSoulToKatana,arkemile);
			}else if(source == tsaraksoul){
				turnOff(lichdrain,kharimsoul,ripSoulToKatana,arkemile);
			}else if(source == ripSoulToKatana){
				turnOff(lichdrain,kharimsoul,tsaraksoul,arkemile);
			}else if(source == arkemile){
				turnOff(lichdrain,kharimsoul,tsaraksoul,ripSoulToKatana, eatCorpse, barbarianBurn, feedCorpseTo, 
						beheading, desecrateGround, burialCere, lichWake, skeletonWake, vampireWake, zombieWake, aelenaFam, aelenaOrgan);
			}else if(source == eatCorpse){
				turnOff(arkemile,eatCorpse, ripSoulToKatana, barbarianBurn, feedCorpseTo,beheading, desecrateGround, burialCere, lichWake, skeletonWake, vampireWake, zombieWake, aelenaFam, aelenaOrgan);
			}else if(source == barbarianBurn){
				turnOff(arkemile, eatCorpse,ripSoulToKatana, feedCorpseTo,beheading, desecrateGround, burialCere, lichWake, skeletonWake, vampireWake, zombieWake, aelenaFam, aelenaOrgan);
			}else if(source == feedCorpseTo){
				turnOff(arkemile,eatCorpse, ripSoulToKatana, barbarianBurn,beheading, desecrateGround, burialCere, lichWake, skeletonWake, vampireWake, zombieWake, aelenaFam, aelenaOrgan);
			}else if(source == beheading){
				turnOff(arkemile,eatCorpse, ripSoulToKatana, barbarianBurn, feedCorpseTo, desecrateGround, burialCere, lichWake, skeletonWake, vampireWake, zombieWake, aelenaFam, aelenaOrgan);
			}else if(source == desecrateGround){
				turnOff(arkemile, eatCorpse,ripSoulToKatana, barbarianBurn, feedCorpseTo,beheading,burialCere, lichWake, skeletonWake, vampireWake, zombieWake, aelenaFam, aelenaOrgan);
			}else if(source == burialCere){
				turnOff(arkemile,eatCorpse, ripSoulToKatana, barbarianBurn, feedCorpseTo,beheading, desecrateGround,  lichWake, skeletonWake, vampireWake, zombieWake, aelenaFam, aelenaOrgan);
			}else if(source == lichWake){
				turnOff(arkemile, eatCorpse,ripSoulToKatana, barbarianBurn, feedCorpseTo,beheading, desecrateGround, burialCere, skeletonWake, vampireWake, zombieWake, aelenaFam, aelenaOrgan);
			}else if(source == vampireWake){
				turnOff(arkemile, eatCorpse,ripSoulToKatana, barbarianBurn, feedCorpseTo,beheading, desecrateGround, burialCere, lichWake, skeletonWake, zombieWake, aelenaFam, aelenaOrgan);
			}else if(source == skeletonWake){
				turnOff(arkemile,eatCorpse, ripSoulToKatana, barbarianBurn, feedCorpseTo,beheading, desecrateGround, burialCere, lichWake,  vampireWake, zombieWake, aelenaFam, aelenaOrgan);
			}else if(source == zombieWake){
				turnOff(arkemile,eatCorpse, ripSoulToKatana, barbarianBurn, feedCorpseTo,beheading, desecrateGround, burialCere, lichWake, skeletonWake, vampireWake, aelenaFam, aelenaOrgan);
			}else if(source == aelenaFam){
				turnOff(arkemile,eatCorpse, ripSoulToKatana, barbarianBurn, feedCorpseTo,beheading, desecrateGround, burialCere, lichWake, skeletonWake, vampireWake, aelenaOrgan);
			}else if(source == aelenaOrgan){
				turnOff(arkemile,eatCorpse, ripSoulToKatana, barbarianBurn, feedCorpseTo,beheading, desecrateGround, burialCere, lichWake, skeletonWake, vampireWake, aelenaFam);
			}else if (source == wakeFollow){
				turnOff( wakeAgro, wakeTalk, wakeStatic);
			}else if (source == wakeAgro){
				turnOff(wakeFollow,  wakeTalk, wakeStatic);
			}else if (source == wakeTalk){
				turnOff(wakeFollow, wakeAgro,  wakeStatic);
			}else if (source == wakeStatic){
				turnOff(wakeFollow, wakeAgro, wakeTalk);
			}else if (source == clear){

				int confirmation = JOptionPane.showConfirmDialog(null, "Sure you want to clear everything?");
				if(confirmation==0){
					this.model.clear();
					this.loadFromModel();
				}
	
			}

			saveToModel();
			plugin.saveRipAction(makeRipString());
	}

	private void turnOff(CorpseCheckBox ... boxes){
		for(CorpseCheckBox box : boxes){
			box.setSelected(false);
		}
	}

	private String makeRipString() {
		String rip = "";
		
		if(lichdrain.isSelected()){
			rip+=lichdrain.getEffect()+this.model.getDelim();
		}
		if(kharimsoul.isSelected()){
			rip+=kharimsoul.getEffect()+this.model.getDelim();
		}
		if(kharimSoulCorpse.isSelected()){
			rip+=kharimSoulCorpse.getEffect()+this.model.getDelim();
		}
		if(tsaraksoul.isSelected()){
			rip+=tsaraksoul.getEffect()+this.model.getDelim();
		}
		if(ripSoulToKatana.isSelected()){
			rip+=ripSoulToKatana.getEffect()+this.model.getDelim();
		}
		if(arkemile.isSelected()){
			rip+=arkemile.getEffect()+this.model.getDelim();
		}
		if(gac.isSelected()){
			rip+=gac.getEffect()+this.model.getDelim();
		}
		if(ga.isSelected()){
			rip+=ga.getEffect()+this.model.getDelim();
		}
		if(eatCorpse.isSelected()){
			rip+=eatCorpse.getEffect()+this.model.getDelim();
		}
		if(donate.isSelected()){
			rip+=donate.getEffect()+this.model.getDelim();
		}
		if(lootCorpse.isSelected()){
			rip+=lootCorpse.getEffect()+this.model.getDelim();
		}
		if(lootGround.isSelected()){
			rip+=lootGround.getEffect()+this.model.getDelim();
		}
		if(barbarianBurn.isSelected()){
			rip+=barbarianBurn.getEffect()+this.model.getDelim();
		}
		if(feedCorpseTo.isSelected()){
			rip+=feedCorpseTo.getEffect()+this.model.getDelim();
		}
		if(beheading.isSelected()){
			rip+=beheading.getEffect()+this.model.getDelim();
		}
		if(desecrateGround.isSelected()){
			rip+=desecrateGround.getEffect()+this.model.getDelim();
		}
		if(burialCere.isSelected()){
			rip+=burialCere.getEffect()+this.model.getDelim();
		}
		if(dig.isSelected()){
			rip+=dig.getEffect()+this.model.getDelim();
		}
		if(wakeFollow.isSelected()){
			rip+=wakeFollow.getEffect()+this.model.getDelim();
		}
		if(wakeAgro.isSelected()){
			rip+=wakeAgro.getEffect()+this.model.getDelim();
		}
		if(wakeTalk.isSelected()){
			rip+=wakeTalk.getEffect()+this.model.getDelim();
		}
		if(wakeStatic.isSelected()){
			rip+=wakeStatic.getEffect()+this.model.getDelim();
		}
		if(lichWake.isSelected()){
			rip+=lichWake.getEffect()+this.model.getDelim();
		}
		if(vampireWake.isSelected()){
			rip+=vampireWake.getEffect()+this.model.getDelim();
		}
		if(skeletonWake.isSelected()){
			rip+=skeletonWake.getEffect()+this.model.getDelim();
		}
		if(zombieWake.isSelected()){
			rip+=zombieWake.getEffect()+this.model.getDelim();
		}
		if(aelenaFam.isSelected()){
			rip+=aelenaFam.getEffect()+this.model.getDelim();
		}
		if(aelenaOrgan.isSelected()){
			rip+=aelenaOrgan.getEffect()+" "+this.model.getOrgan1()+" "+this.model.getOrgan2()+this.model.getDelim();
		}
		
		
		
		rip = rip.substring(0, rip.length()-this.model.getDelim().length());
		return rip;
	}
	


	@Override
	public void componentHidden(ComponentEvent arg0) {
	}


	@Override
	public void componentMoved(ComponentEvent arg0) {	
	}


	@Override
	public void componentResized(ComponentEvent arg0) {
		redoLayout();
	}


	@Override
	public void componentShown(ComponentEvent arg0) {
	}
	
	private void redoLayout() {
		this.setLayout(null);
//		private Border whiteline = BorderFactory.createLineBorder(Color.white);
		soulPanel.setBorder(BorderFactory.createTitledBorder(whiteline, "Souls", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, font, TEXT_COLOR));
		soulPanel.setBackground(Color.BLACK);
		listPanel.setBorder(BorderFactory.createTitledBorder(whiteline, "Items to loot", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, font, TEXT_COLOR));
		listPanel.setBackground(Color.BLACK);
		controlPanel.setBorder(BorderFactory.createTitledBorder(whiteline, "Controls", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, font, TEXT_COLOR));
		controlPanel.setBackground(Color.BLACK);
		wakePanel.setBorder(BorderFactory.createTitledBorder(whiteline, "Wake up corpse!", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, font, TEXT_COLOR));
		wakePanel.setBackground(Color.BLACK);
		lootPanel.setBorder(BorderFactory.createTitledBorder(whiteline, "Looting", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, font, TEXT_COLOR));
		lootPanel.setBackground(Color.BLACK);
		corpsePanel.setBorder(BorderFactory.createTitledBorder(whiteline, "Carcasses", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, font, TEXT_COLOR));
		corpsePanel.setBackground(Color.BLACK);
		
		soulPanel.setBounds(BORDERLINE*2, BORDERLINE*2, (CB_WIDTH*2)+(2*TOP_BORDER) , (CB_HEIGHT*4));
			soulPanel.setLayout(null);
			kharimsoul.setBounds(BORDERLINE,TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			kharimSoulCorpse.setBounds(CB_WIDTH+BORDERLINE,TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			lichdrain.setBounds(BORDERLINE,CB_HEIGHT+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			tsaraksoul.setBounds(CB_WIDTH+BORDERLINE,CB_HEIGHT+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			ripSoulToKatana.setBounds(BORDERLINE,(CB_HEIGHT*2)+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			arkemile.setBounds(CB_WIDTH+BORDERLINE,(CB_HEIGHT*2)+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			soulPanel.add(kharimsoul);
			soulPanel.add(kharimSoulCorpse);
			soulPanel.add(lichdrain);
			soulPanel.add(tsaraksoul);
			soulPanel.add(ripSoulToKatana);
			soulPanel.add(arkemile);
		this.add(soulPanel);
		
		corpsePanel.setBounds(BORDERLINE*2, soulPanel.getHeight()+(BORDERLINE*4), (CB_WIDTH*2)+(2*TOP_BORDER) , (CB_HEIGHT*5));
		corpsePanel.setLayout(null);
			barbarianBurn.setBounds(BORDERLINE, TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			feedCorpseTo.setBounds(CB_WIDTH+BORDERLINE, TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			beheading.setBounds(BORDERLINE, CB_HEIGHT+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			desecrateGround.setBounds(CB_WIDTH+BORDERLINE, CB_HEIGHT+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			burialCere.setBounds(BORDERLINE, (CB_HEIGHT*2)+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			dig.setBounds(CB_WIDTH+BORDERLINE, (CB_HEIGHT*2)+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			aelenaOrgan.setBounds(BORDERLINE, (CB_HEIGHT*3)+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			aelenaFam.setBounds(CB_WIDTH+BORDERLINE, (CB_HEIGHT*3)+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			corpsePanel.add(barbarianBurn);
			corpsePanel.add(feedCorpseTo);
			corpsePanel.add(beheading);
			corpsePanel.add(desecrateGround);
			corpsePanel.add(burialCere);
			corpsePanel.add(dig);
			corpsePanel.add(aelenaOrgan);
			corpsePanel.add(aelenaFam);
			
		this.add(corpsePanel);
		
		wakePanel.setBounds(BORDERLINE*2, corpsePanel.getHeight()+soulPanel.getHeight()+(BORDERLINE*6), (CB_WIDTH*2)+(2*TOP_BORDER) , (CB_HEIGHT*5));
		wakePanel.setLayout(null);
			lichWake.setBounds(BORDERLINE,TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			skeletonWake.setBounds(BORDERLINE,CB_HEIGHT+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			zombieWake.setBounds(BORDERLINE,(CB_HEIGHT*2)+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			vampireWake.setBounds(BORDERLINE,(CB_HEIGHT*3)+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			wakeFollow.setBounds(CB_WIDTH+BORDERLINE,TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			wakeAgro.setBounds(CB_WIDTH+BORDERLINE,CB_HEIGHT+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			wakeTalk.setBounds(CB_WIDTH+BORDERLINE,(CB_HEIGHT*2)+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			wakeStatic.setBounds(CB_WIDTH+BORDERLINE,(CB_HEIGHT*3)+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			wakePanel.add(wakeFollow);
			wakePanel.add(wakeAgro);
			wakePanel.add(wakeTalk);
			wakePanel.add(wakeStatic);
			wakePanel.add(lichWake);
			wakePanel.add(vampireWake);
			wakePanel.add(skeletonWake);
			wakePanel.add(zombieWake);
		this.add(wakePanel);

		lootPanel.setBounds(BORDERLINE*2, wakePanel.getHeight()+corpsePanel.getHeight()+soulPanel.getHeight()+(BORDERLINE*8), (CB_WIDTH*2)+(2*TOP_BORDER) , (CB_HEIGHT*5));
		lootPanel.setLayout(null);
			gac.setBounds(BORDERLINE,TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			ga.setBounds(BORDERLINE,CB_HEIGHT+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			eatCorpse.setBounds(BORDERLINE,(CB_HEIGHT*2)+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			donate.setBounds(BORDERLINE,(CB_HEIGHT*3)+TOP_BORDER, CB_WIDTH*2, CB_HEIGHT);
			lootCorpse.setBounds(CB_WIDTH+BORDERLINE,TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			lootGround.setBounds(CB_WIDTH+BORDERLINE,CB_HEIGHT+TOP_BORDER, CB_WIDTH, CB_HEIGHT);
			lootPanel.add(gac);
			lootPanel.add(ga);
			lootPanel.add(eatCorpse);
			lootPanel.add(donate);
			lootPanel.add(lootCorpse);
			lootPanel.add(lootGround);
		
		this.add(lootPanel);

		controlPanel.setBounds((BORDERLINE*4)+lootPanel.getWidth(),BORDERLINE*2, (LABEL_WIDTH*2)+(2*TOP_BORDER) , (CB_HEIGHT*9) );
		controlPanel.setLayout(null);
			
		
			on.setBounds(BORDERLINE,TOP_BORDER, BUTTON_WIDTH, CB_HEIGHT);
			on.setBackground(BG_COLOR);
			on.setForeground(TEXT_COLOR);
			off.setBounds(BORDERLINE,CB_HEIGHT+TOP_BORDER, BUTTON_WIDTH, CB_HEIGHT);
			off.setBackground(BG_COLOR);
			off.setForeground(TEXT_COLOR);
			delimLabel.setBounds(BORDERLINE, (CB_HEIGHT*3)+TOP_BORDER, LABEL_WIDTH, CB_HEIGHT);
			delimLabel.setForeground(TEXT_COLOR);
			delim.setBounds(LABEL_WIDTH+BORDERLINE, (CB_HEIGHT*3)+TOP_BORDER, LABEL_WIDTH, CB_HEIGHT);
			mountLabel.setBounds(BORDERLINE, (CB_HEIGHT*4)+TOP_BORDER, LABEL_WIDTH, CB_HEIGHT);
			mountLabel.setForeground(TEXT_COLOR);
			mount.setBounds(LABEL_WIDTH+BORDERLINE, (CB_HEIGHT*4)+TOP_BORDER, LABEL_WIDTH, CB_HEIGHT);
			organ1Label.setBounds(BORDERLINE, (CB_HEIGHT*5)+TOP_BORDER, LABEL_WIDTH, CB_HEIGHT);
			organ1Label.setForeground(TEXT_COLOR);
			organ1.setBounds(LABEL_WIDTH+BORDERLINE, (CB_HEIGHT*5)+TOP_BORDER, LABEL_WIDTH, CB_HEIGHT);
			organ2Label.setBounds(BORDERLINE, (CB_HEIGHT*6)+TOP_BORDER, LABEL_WIDTH, CB_HEIGHT);
			organ2Label.setForeground(TEXT_COLOR);
			organ2.setBounds(LABEL_WIDTH+BORDERLINE, (CB_HEIGHT*6)+TOP_BORDER, LABEL_WIDTH, CB_HEIGHT);
			clear.setBounds((5*BORDERLINE)+LABEL_WIDTH, TOP_BORDER, BUTTON_WIDTH, CB_HEIGHT);
			controlPanel.add(on);
			controlPanel.add(off);
			controlPanel.add(delim);
			controlPanel.add(mount);
			controlPanel.add(clear);
			controlPanel.add(organ1);
			controlPanel.add(organ2);
			controlPanel.add(delimLabel);
			controlPanel.add(mountLabel);
			controlPanel.add(organ1Label);
			controlPanel.add(organ2Label);
			controlPanel.add(clear);
		this.add(controlPanel);

		listPanel.setBounds((BORDERLINE*4)+lootPanel.getWidth(), controlPanel.getHeight()+(BORDERLINE*4), (LABEL_WIDTH*2)+(2*TOP_BORDER), (CB_HEIGHT*11));
		listPanel.setLayout(null);
			lootLists.setBounds(BORDERLINE*2, TOP_BORDER, listPanel.getWidth()-(4*BORDERLINE) , listPanel.getHeight()-(2*TOP_BORDER));
			listPanel.add(lootLists);
		this.add(listPanel);

	}
	
}
