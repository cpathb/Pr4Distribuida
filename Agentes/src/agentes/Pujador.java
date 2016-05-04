//package agentes;

/**
 *
 * @author icarli
 */

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.HashMap;

public class Pujador extends Agent{
    // Titulos de los objeto a comprar y su precio
    private HashMap<String,Integer> objetivos=new HashMap();

    // Lista de los Subastadores conocidos
    private AID[] subastadores;

    // Método para la inicialización del agente
    protected void setup() {
        // Imprimimos un mensaje de bienvenida
        System.out.println("Hola! Soy " + getAID().getName() + " y estoy listo para la fiesta!");

        // Obtenemos el título del objetivo de los argumentos de entrada del programa
        Object[] args = getArguments();
        //if (args != null && args.length > 0) {
            //objetivos.put((String) args[0],30);
        if (args != null && args.length >= 0) {
            objetivos.put("A",10);
            
            // Añadimos un comportamiento TickerBehaviour que planifique peticiones de informacion a los subastadores cada cierto tiempo
            addBehaviour(new TickerBehaviour(this, 10000) {
                protected void onTick(){
                    int i=0;
                    // Consultamos con el servicio de páginas amarillas para conocer los subastadores
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("Subastador");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        System.out.println("Se encontraron los siguientes subastadores:");
                        subastadores = new AID[result.length];
                        while(i < result.length) {
                            subastadores[i] = result[i].getName(); // sin el .getName()
                            System.out.println(subastadores[i].getName());
                            i++;
                        }
                        if(!objetivos.isEmpty()){
                            myAgent.addBehaviour(new manejadorRespuestas());
                        }
                    }
                    catch (FIPAException fe) {
                        fe.printStackTrace();
                    }
                }
            });
        }
        else {
            // Si no se especifica un libro terminamos la ejecución del agente
            System.out.println("No se ha especificado el título para el objetivo, ahora me enfado y no respiro");
            doDelete();
        }
    }

    // Acciones a ejecutar al invocar al doDelete()
    protected void takeDown() {
        // Imprimimos un mensaje de despedida
        System.out.println("Pujador " + getAID().getName() + " finalizando");
    }

    /**
     * Clase manejadorRespuestas. Este es el comportamiento usado por el Pujador
     * para solicitar los libros objetivos a los Subastadores
     */
    private class manejadorRespuestas extends Behaviour {

        private AID mejorSubastador; // Subastador con el mejor precio
        private int mejorPrecio;  // Mejor precio
        private int countRespuestas = 0; // Contador para las respuestas de consultas
        private MessageTemplate mt; // Plantilla para recibir las respuestas
        private int step = 0;

        public void action() {
            int i=0;
            switch (step) {
                case 0: // Enviamos las peticiones y preparamos las plantillas para las respuestas
                    // Enviar el CALL FOR PROPOSAL a los Subastadores
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    while(i < subastadores.length){
                        cfp.addReceiver(subastadores[i]);
                        i++;
                    }
                    i=0;

                    cfp.setConversationId("Consulta");
                    cfp.setContent("A");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Valor unico
                    myAgent.send(cfp);
                    // Preparamos la plantilla para las respuestas
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Consulta"),MessageTemplate.MatchInReplyTo(cfp.getReplyWith())); // Filtramos por el identificador de la conversacion
                    step = 1;
                    break;
                case 1: // Recibimos las respuestas de los Subastadores
                    ACLMessage respuesta = myAgent.receive(mt); // Recibimos los mensajes que concuerdan con la plantilla
                    if (respuesta != null) { // Comprobamos si hemos recibido respuestas
                        if (respuesta.getPerformative() == ACLMessage.PROPOSE) { // Un Subastador contiene el libro que buscamos
                            int precio = Integer.parseInt(respuesta.getContent());
                            if ((mejorSubastador == null  && precio<=objetivos.get("A")) || (precio < mejorPrecio && precio<=objetivos.get("A"))) { // Comprobamos si no tenemos un mejor vendedor o el mejor precio es mayor al actual, si es asi:
                                // Se cambia la mejor subasta con la actual
                                mejorPrecio = precio;
                                mejorSubastador = respuesta.getSender();
                            }
                        }
                        countRespuestas++;
                        if (countRespuestas >= subastadores.length) { // Se recibieron todas las respuestas
                            step = 2;
                        }
                    } else { // No recibimos ninguna respuesta
                        block();
                    }
                    break;
                case 2: // Una vez recibidas todas las respuestas, tenemos al mejor subastador y le enviamos una puja
                    // Enviamos una puja al mejor subastador
                    ACLMessage puja = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    puja.addReceiver(mejorSubastador);
                    puja.setConversationId("Puja:"+"A");
                    puja.setContent(""+mejorPrecio);
                    puja.setReplyWith("puja" + System.currentTimeMillis());
                    myAgent.send(puja);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Puja:"+"A"),MessageTemplate.MatchInReplyTo(puja.getReplyWith()));
                    step = 3;
                    break;
                case 3: // Recibimos la respuesta a nuestra puja
                    respuesta = myAgent.receive(mt);

                    if (respuesta != null) {
                        if (respuesta.getPerformative() == ACLMessage.INFORM) { // Hemos ganado la ronda de pujas
                            System.out.println(respuesta.getContent());
                        }
                        else { // No somos los ganadores de la ronda de pujas
                            System.out.println(respuesta.getContent());
                        }
                        step = 4;
                    } else {
                        block();
                    }
                    break;
                case 4:
                    if(objetivos.isEmpty()){
                        step=5;
                    }
            }
        }

        public boolean done(){
            if (step == 2 && mejorSubastador == null) {
                if(step==0){
                    System.out.println("No hay una subasta del objeto disponible");
                }
                if(step==3){
                    System.out.println("Pujaaaa!!");
                }
            }
            return ((step == 2 && mejorSubastador == null) || step == 5);
        }
    }
}
