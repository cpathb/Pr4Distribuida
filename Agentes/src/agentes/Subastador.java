//package agentes;

/**
 *
 * @author icarli
 */

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.*;

public class Subastador extends Agent{
    // La lista de objetos a subastar
    private HashMap<String, Subasta> subastas;
    // Interfaz de usuario para el agente
    //private interfazSubastador interfaz;

    // Método para la inicialización del agente
    protected void setup() {
        // Creamos la lista donde estarán los objetos a subastar
        subastas = new HashMap();
        subastas.put("A", new Subasta("A",null,10,10,10));
        subastas.put("B", new Subasta("B",null,100,100,15));
        // Creamos y mostramos la interfaz de usuario 
        //interfaz = new interazSubastador(this);
        //interfaz.showGui(); o interfaz.mostrar(); // Comprobar si el showGUI existe
        // Registramos al subastador en el servicio de paginas amarillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Subastador");
        sd.setName("Agente-Subastador-JADE");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Por defecto añadimos los comportamientos para las consultas de elementos en subasta, manejar las subastas y la notificación a los clientes de las subastas del precio actual para pujar
        addBehaviour(new consultas());
        addBehaviour(new manejarSubastas());
        //addBehaviour(new notificar());
    }

    // Acciones a ejecutar al invocar al doDelete()
    protected void takeDown() {
        // Deregistramos el agente de las páginas amarillas
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Cerramos la interfaz
        //interfaz.dispose();
        // Mensaje de sálida
        System.out.println("Subastador " + getAID().getName() + " finalizando");
    }

    /**
     * Método invocado en la interfaz del subastador para añadir los objetos a la subasta, estos objetos contendrán un titulo y un precio
     */
    public void modificarSubastas(final String titulo, final String nombreGanador, final int precio, int incremento){
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                if(!subastas.containsKey(titulo)){
                    subastas.put(titulo, new Subasta(titulo,null,precio,precio,incremento));
                    System.out.println(titulo + " insertado en las subastas. Precio de sálida = " + precio);
                }
                else{
                    System.out.println("El libro '"+titulo + "' ya está a subasta");
                }
            }
        });
    }

    /**
     * Clase consultas. Este comportamiento es usado por el Subastador para manejar 
     * las consultas de objetos por parte de los Pujadores.
     * Si el Subastador tiene el objeto en subasta contestará con un mensaje PROPOSE indicando
     * el precio actual de la puja de ese objeto, sino, contestara conun mensaje REFUSE.
     */
    private class consultas extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) { // Comprobamos si se ha recibido una consulta por un objeto de la subasta
                String titulo = msg.getContent(); // Obtenemos el título del objeto del mensaje
                ACLMessage respuesta = msg.createReply(); // Creamos la respuesta                
                // Obtenemos el precio del libro por el que se pregunta
                if (subastas.containsKey(titulo)) { // Si el precio es distinto de nulo es que existe el objeto a subasta y devolvemos el precio
                    respuesta.setPerformative(ACLMessage.PROPOSE); // Añadimos el tipo del mensaje
                    respuesta.setContent(String.valueOf(subastas.get(titulo).getPrecioActual())); // Añadimos el valor del mensaje
                } else { // Si no existe el libro envimos un mensaje informando de que no lo tenemos disponible una subasta para este
                    respuesta.setPerformative(ACLMessage.REFUSE);
                    respuesta.setContent("No tenemos una subasta para '"+titulo+"'");
                }
                myAgent.send(respuesta); // Enviamos la respuesta
            }
            else { // Si no se recibieron solicitudes de información pasamos al siguiente comportamiento
                block();
            }
        }
    }

    /**
     * Clase manejarSubastas. Este comportamiento es usado para manejar el flujo de las subastas
     * entre sus tareas está:
     * Controlar a los que abandonan la subasta
     * Controlar las pujas a objetos en subasta
     * Informar de si se ha sido el ganador o no de la ronda de pujasnn                                   
     */
    private class manejarSubastas extends CyclicBehaviour{
        public void action(){
            MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL), MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) { // Comprobamos si llego alguna puja o una renuncia a una puja
                if(msg.getPerformative()==ACLMessage.REJECT_PROPOSAL){ // Renuncia a la puja
                    String titulo = msg.getConversationId().replace("Puja:", ""); // Obtenemos el título del objeto de la puja
                    Subasta subasta= subastas.get(titulo); // Obtenemos la subasta correspondiente de la tabla hash
                    subasta.getPujadores().remove(msg.getSender()); // Eliminamos el pujador de nuestra subasta
                    ACLMessage respuesta = msg.createReply(); // Creamos la respuesta                
                    respuesta.setPerformative(ACLMessage.INFORM); // Comprobar esta performativa!!
                    respuesta.setContent(msg.getSender().getName()+", te has retirado de la puja por el objeto:" +titulo+" recuerda que si eras la puja ganadora puedes ganar el libro aunque te retires");
                    subastas.replace(titulo, subasta);
                }
                else{ // Puja a un objeto
                    ACLMessage respuesta = msg.createReply(); // Creamos la respuesta                
                    String titulo = msg.getConversationId().replace("Puja:", "");
                    Subasta subasta=subastas.get(titulo);
                    int preciosub=subasta.getPrecioActual();
                    int precio = Integer.parseInt(msg.getContent());
                    
                    System.out.println(precio);
                    System.out.println(preciosub);
                    System.out.println(precio==preciosub);
                    
                    if (subasta.getGanador()!=null){ // Comprobamos si no hay todavia un ganador
                        if(msg.getSender().getName().compareTo(subasta.getGanador().getName())!=0){ // Comprobamos si la puja no es del actual ganador
                            if(!subasta.getPujadores().contains(msg.getSender())){ // Comprobamos si la subasta contiene a ese pujador, si no lo tiene lo añadimos
                                subasta.getPujadores().add(msg.getSender()); 
                            }
                            if (preciosub == precio){
                                respuesta.setPerformative(ACLMessage.INFORM);
                                respuesta.setContent(msg.getSender().getName()+" vas en cabeza de la puja por: "+titulo);
                                subasta.setPrecioActual(precio+subasta.getIncremento());
                                subasta.setGanador(msg.getSender());
                                subastas.replace(titulo, subasta); // Modificamos el objeto en el hashmap
                            }
                            else {
                                // Alguien se ha adelantado en la puja
                                respuesta.setPerformative(ACLMessage.INFORM);
                                respuesta.setContent("Lo siento, "+subasta.getGanador().getName()+" va en cabeza");
                            }
                        }
                    }
                    else{
                        if(!subasta.getPujadores().contains(msg.getSender())){ // Comprobamos si la subasta contiene a ese pujador, si no lo tiene lo añadimos
                            subasta.getPujadores().add(msg.getSender()); 
                        }
                        if (preciosub == precio){
                            respuesta.setPerformative(ACLMessage.INFORM);
                            respuesta.setContent(msg.getSender().getName()+" vas en cabeza de la puja por: "+titulo);
                            subasta.setPrecioActual(precio+subasta.getIncremento());
                            subasta.setGanador(msg.getSender());
                            subastas.replace(titulo, subasta); // Modificamos el objeto en el hashmap
                        }
                        else { // Alguien se ha adelantado en la puja
                            respuesta.setPerformative(ACLMessage.INFORM);
                            respuesta.setContent("Lo siento, hay una oferta superior");
                        }
                    }
                    myAgent.send(respuesta);
                }
            }
            else {
                block();
            }
        }
    }
}