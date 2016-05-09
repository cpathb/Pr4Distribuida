//package agentes;

/**
 *
 * @author icarli
 */

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import java.util.LinkedList;
import javax.swing.DefaultListModel;

public class Subastador extends Agent{
    // La lista de objetos a subastar
    private HashMap<String, Subasta> subastas;
    // Interfaz de usuario para el agente
    private interfazSubastador interfaz;

    // Lista de los Subastadores conocidos
    private AID[] pujadores;


    // Método para la inicialización del agente
    protected void setup() {
        // Creamos la lista donde estarán los objetos a subastar
        subastas = new HashMap();

        // Creamos y mostramos la interfaz de usuario
        interfaz = new interfazSubastador(this);
        interfaz.mostrar(); // Comprobar si el showGUI existe

        // Añadimos un comportamiento TickerBehaviour que planifique los envios de peticiones de participacion a los pujadores cada cierto tiempo
        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick(){
                /*
                    Consultamos si para cada una de las subastas que tenemos en funcionamiento se cumplen los requisitos de finalización
                    Un solo pujador o si no hay pujadores y hay una puja potencial ganadora ese es el ganador
                    Precio al finalizar la subasta sea mayor o igual al minimo de la subasta
                */
                Subasta subasta;
                LinkedList<String> borrar=new LinkedList<String>();
                for(String titulo : subastas.keySet()){ // Bucle comprobacion de subastas
                    subasta=subastas.get(titulo);
                   if(subasta.getPujadores().size()<=1){ // Comprobamos si hay tan solo uno o ningún pujador
                       if(subasta.getGanadorActual()!= null && (subasta.getPrecio()-subasta.getIncremento())>=subasta.getMinimo()){ // Si hay un pujador activo
                           ACLMessage finalizacion=new ACLMessage(ACLMessage.REQUEST);
                           finalizacion.addReceiver(subasta.getGanadorActual());
                           finalizacion.setContent(titulo+"$"+(subasta.getPrecio()-subasta.getIncremento()));
                           myAgent.send(finalizacion); // Enviamos el mensaje para indicar que ha ganado
                           borrar.add(titulo);
                       }
                   }
                }
                while(!borrar.isEmpty()){ // Borramos los objetos que hayan sido resueltos
                    eliminarSubasta(borrar.get(0));
                    borrar.remove(0);
                }

                int i=0;
                // Consultamos con el servicio de páginas amarillas para conocer los subastadores
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("Pujador");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    pujadores = new AID[result.length];
                    while(i < result.length){
                        pujadores[i] = result[i].getName();
                        i++;
                    }
                    if(!subastas.isEmpty()){
                        myAgent.addBehaviour(new manejadorSubastas());
                    }
                }
                catch (FIPAException fe){
                    fe.printStackTrace();
                }
            }
        });
    }

    // Acciones a ejecutar al invocar al doDelete()
    protected void takeDown() {
         // Mensaje de sálida
        interfaz.añadirMensaje("Subastador " + getAID().getName() + " finalizando");
        // Cerramos la interfaz
        interfaz.dispose();

    }

    /**
     * Clase manejadorSubastas. Este es el comportamiento usado por el Subastador
     * para enviar las invitaciones de las subastas a los pujadores que se detectaron
     * y para llevar a cabo el manejo de las rondas de pujas de las subastas
     */
    private class manejadorSubastas extends Behaviour {
        private int countRespuestas = 0; // Contador para las respuestas de consultas
        private MessageTemplate mt; // Plantilla para recibir las respuestas
        private int step = 0;

        public void action() {
            Subasta subasta;
            switch (step) {
                case 0: // Enviamos las invitaciones para pujar en la subasta a los clientes recogidos del servicio de paginas amarillas
                    // Enviar el Call For Proposal a los pujadores encontrados en el tickerBehaviour
                    for(String tit:subastas.keySet()){ // Para cada elemento en subasta hacemos las siguientes acciones
                        subasta=subastas.get(tit);
                        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                        for(int i=0;i < pujadores.length;i++) {
                            cfp.addReceiver(pujadores[i]); // Añadimos como destinatarios todos los pujadores detectados
                        }
                        cfp.setConversationId("Invitacion"); // Añadimos un id al mensaje
                        cfp.setContent(tit+"$"+subasta.getPrecio());
                        cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Valor unico
                        myAgent.send(cfp);
                    }
                    // Preparamos la plantilla para las respuestas
                    mt = MessageTemplate.or(MessageTemplate.MatchConversationId("Invitacion"),MessageTemplate.MatchConversationId("Baja")); // Filtramos por el identificador de la conversacion
                    step = 1;
                    break;

                case 1: // Recibimos las respuestas a los mensajes de invitacion de puja que enviamos
                    ACLMessage respuesta = myAgent.receive(mt); // Recibimos los mensajes que concuerdan con la plantilla
                    if (respuesta != null) { // Comprobamos si hemos recibido las respuestas
                        if (respuesta.getPerformative() == ACLMessage.PROPOSE) { // Un pujador puja por el objeto que le propusimos
                            String contenido = respuesta.getContent(); // Obtenemos el título del objeto del mensaje
                            String[] partes=contenido.split("\\$");
                            String titulo=partes[0];
                            int precio= Integer.parseInt(partes[1]);
                            subasta=subastas.get(titulo);
                            LinkedList<AID> pujadores=subasta.getPujadores();
                            if(!pujadores.contains(respuesta.getSender())){ // Si todavía no está registrado como pujador lo registramos
                                pujadores.add(respuesta.getSender()); // Registramos como pujador
                                subasta.setPujadores(pujadores);
                            }
                            if(precio==subasta.getPrecio()){
                                subasta.setPrecio(precio+subasta.getIncremento()); // Actualizamos al nuevo precio a pedir
                                subasta.setGanadorActual(respuesta.getSender()); // Actualizamos el ganador de la subasta
                                interfaz.añadirMensaje("Recibida una puja ganadora de " + respuesta.getSender().getName()+ " para "+titulo + " por "+precio);
                            }
                            else{
                                interfaz.añadirMensaje("Recibida una puja perdedora de " + respuesta.getSender().getName()+ " para "+titulo + " por "+precio);
                            }
                            ACLMessage notificacion = new ACLMessage(ACLMessage.INFORM);
                            notificacion.addReceiver(respuesta.getSender());
                            notificacion.setConversationId("Ronda");
                            notificacion.setContent(titulo+"$"+(subasta.getPrecio()-subasta.getIncremento())+"$"+subasta.getGanadorActual().getName());
                            myAgent.send(notificacion);
                            subastas.replace(titulo,subasta); // Cambiamos el objeto por el actualizado
                        }
                        if (respuesta.getPerformative() == ACLMessage.REFUSE) { // Un pujador se retira de la puja por el objeto que le propusimos
                            String contenido = respuesta.getContent(); // Obtenemos el título del objeto del mensaje y el precio
                            String[] partes=contenido.split("\\$");
                            String titulo=partes[0];
                            int precio= Integer.parseInt(partes[1]);
                            subasta=subastas.get(titulo);
                            LinkedList<AID> pujadores=subasta.getPujadores();
                            if(pujadores.contains(respuesta.getSender())){ // Si todavía está registrado como pujador lo desregistramos
                                pujadores.remove(respuesta.getSender()); // Desregistramos como pujador
                                subasta.setPujadores(pujadores);
                                if(respuesta.getConversationId().compareTo("Baja")==0){ // Si estaba en la puja y se dio de baja lo avisamos de que fue retirado
                                    interfaz.añadirMensaje(respuesta.getSender().getName()+ " se ha dado de baja en la puja para "+titulo);
                                    ACLMessage notificacion = new ACLMessage(ACLMessage.INFORM);
                                    notificacion.addReceiver(respuesta.getSender());
                                    notificacion.setConversationId("Baja");
                                    notificacion.setContent(titulo+"$"+precio);
                                    myAgent.send(notificacion);
                                }
                            }
                            subastas.replace(titulo,subasta); // Cambiamos el objeto por el actualizado
                        }
                        countRespuestas++;
                        if (countRespuestas >= (pujadores.length*subastas.size())) { // Se recibieron todas las respuestas
                            step = 2;
                        }
                    }
                    else { // No recibimos ninguna respuesta
                        block();
                    }
                    break;
            }
        }

        public boolean done() {
            return step==2;
        }
    }

    public void crearSubasta(final String titulo, final int precio, int incremento, int minimo){
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                if(!subastas.containsKey(titulo)){
                    subastas.put(titulo, new Subasta(titulo, precio, incremento, minimo));
                    DefaultListModel<String> modelo = new DefaultListModel();
                        subastas.keySet().forEach((s) -> {
                            modelo.addElement(s);
                    });
                    interfaz.listaSubastas(modelo);
                    interfaz.añadirMensaje(titulo + " insertado en las subastas. Precio de sálida " + precio+". Mínimo " + minimo+". Incremento " + incremento+".");
                }
                else{
                    interfaz.añadirMensaje("El objeto "+titulo + " ya está a subasta");
                }
            }
        });
    }
    
    public void eliminarSubasta(final String titulo){
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                if(subastas.containsKey(titulo)){
                    subastas.remove(titulo);
                    DefaultListModel<String> modelo = new DefaultListModel();
                        subastas.keySet().forEach((s) -> {
                            modelo.addElement(s);
                    });
                    interfaz.listaSubastas(modelo);
                    interfaz.añadirMensaje(titulo + " eliminado de nuestra lista de subastas");
                }
            }
        });
    }


}