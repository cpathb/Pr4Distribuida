//package agentes;

/**
 *
 * @author icarli
 */

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import javax.swing.DefaultListModel;

public class Pujador extends Agent{
    // Titulos de los objeto a comprar y su precio
    private HashMap<String,Integer> objetivos;

    // Interfaz de usuario para el agente pujador
    private interfazPujador interfaz;

    // Lista de los Subastadores conocidos
    private AID[] subastadores;

    // Método para la inicialización del agente
    protected void setup(){
        // Creamos la lista donde estarán los objetos a subastar
        objetivos = new HashMap();

        // Creamos y mostramos la interfaz de usuario
        interfaz = new interfazPujador(this);
        interfaz.mostrar(); // Mostramos la interfaz

        // Registramos al pujador en el servicio de paginas amarillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Pujador");
        sd.setName("Agente-Pujador-JADE");
        dfd.addServices(sd);
        try{
            DFService.register(this, dfd);
        }
        catch (FIPAException fe){
            fe.printStackTrace();
        }

        // Añadimos un comportamiento TickerBehaviour que planifique los envios de peticiones de participacion a los pujadores cada cierto tiempo
        addBehaviour(new consultas());

    }

    // Acciones a ejecutar al invocar el doDelete()
    protected void takeDown() {
        // Imprimimos un mensaje de despedida
        interfaz.añadirMensaje("Pujador " + getAID().getName() + " finalizando");
        // Deregistramos el agente pujador de las páginas amarillas
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Cerramos la interfaz
        interfaz.dispose();

    }

    private class consultas extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.CFP),MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchPerformative(ACLMessage.REQUEST)));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) { // Comprobamos si se ha recibido una consulta por un objeto de la subasta
                if(msg.getPerformative()==ACLMessage.CFP){ // Propuesta para pujar en una subasta
                    String contenido = msg.getContent(); // Obtenemos el título del objeto del mensaje
                    String[] partes=contenido.split("\\$");
                    String titulo=partes[0];
                    int precio= Integer.parseInt(partes[1]);
                    ACLMessage respuesta = msg.createReply(); // Creamos la respuesta
                    // Obtenemos el precio del objeto por el que se pregunta
                    if (objetivos.containsKey(titulo)) { // Si el objeto está entre nuestros objetivos
                        if(precio<=objetivos.get(titulo)){ // Podemos permitirnos pujar
                            respuesta.setPerformative(ACLMessage.PROPOSE); // Añadimos el tipo del mensaje
                            respuesta.setContent(titulo+"$"+precio); // Añadimos la puja al contenido
                            myAgent.send(respuesta); // Enviamos la respuesta
                        }
                        else{ // No podemos permitirnos pujar
                            respuesta.setPerformative(ACLMessage.REFUSE);
                            respuesta.setConversationId("Baja");
                            respuesta.setContent(titulo+"$"+precio);
                            myAgent.send(respuesta); // Enviamos la respuesta
                        }
                    }
                    else { // Si no nos interesa el objeto
                        respuesta.setPerformative(ACLMessage.REFUSE);
                        respuesta.setContent(titulo+"$"+precio);
                        myAgent.send(respuesta); // Enviamos la respuesta
                    }
                }
                if(msg.getPerformative()==ACLMessage.REQUEST){ // Ganamos una subasta
                    String contenido = msg.getContent(); // Obtenemos el título del objeto del mensaje
                    String[] partes=contenido.split("\\$");
                    String titulo=partes[0];
                    int precio= Integer.parseInt(partes[1]);
                    eliminarObjeto(titulo);
                    interfaz.añadirMensaje("Has ganado la puja del vendedor "+msg.getSender().getName()+" para: "+titulo+" con un precio de "+precio);
                }
                if(msg.getPerformative()==ACLMessage.INFORM){ // Se acabo una ronda de pujas de una subasta o nos hemos retirado de una puja
                    if(msg.getConversationId().compareTo("Ronda")==0){ // Fin de ronda
                        String contenido = msg.getContent();
                        String[] partes=contenido.split("\\$");
                        String titulo=partes[0];
                        int precio= Integer.parseInt(partes[1]);
                        String ganador=partes[2];
                        if(ganador.compareTo(myAgent.getName())==0){ // Comprobamos si somos nosotros los ganadores
                            interfaz.añadirMensaje("Se ha acabado una ronda de la puja del subastador "+msg.getSender().getName()+", se ha ganado la ronda para: "+titulo+" con una puja de "+precio);
                        }
                        else{
                            interfaz.añadirMensaje("Se ha acabado una ronda de la puja del subastador "+msg.getSender().getName()+", "+ganador+" ha ganado la ronda para: "+titulo+" con una puja de "+precio);
                        }
                    }
                    if(msg.getConversationId().compareTo("Baja")==0) { // Nos hemos dado de baja
                        String contenido = msg.getContent();
                        String[] partes=contenido.split("\\$");
                        String titulo=partes[0];
                        int precio= Integer.parseInt(partes[1]);
                        interfaz.añadirMensaje("Se ha dado de baja en la subasta "+msg.getSender().getName()+" para: "+titulo+" cuando se pujaba a "+precio);
                    }
                }
            }
            else { // Si no se recibieron solicitudes de información pasamos al siguiente comportamiento
                block();
            }
        }
    }

    public void crearObjeto(final String titulo, final int maximo){
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                if(!objetivos.containsKey(titulo)){
                    objetivos.put(titulo, maximo);
                    DefaultListModel<String> modelo = new DefaultListModel();
                        objetivos.keySet().forEach((s) -> {
                            modelo.addElement(s);
                    });
                    interfaz.listaInteres(modelo);
                    
                    interfaz.añadirMensaje(titulo + " añadido a nuesta lista de interes, con un precio máximo de " + maximo);
                }
                else{
                    interfaz.añadirMensaje("El objeto "+titulo + " ya está a subasta");
                }
            }
        });
    }
    
    public void eliminarObjeto(final String titulo){
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                if(objetivos.containsKey(titulo)){
                    objetivos.remove(titulo);
                    DefaultListModel<String> modelo = new DefaultListModel();
                        objetivos.keySet().forEach((s) -> {
                            modelo.addElement(s);
                    });
                    interfaz.listaInteres(modelo);
                    interfaz.añadirMensaje(titulo + " eliminado de nuestra lista de interes");
                }
            }
        });
    }
}
