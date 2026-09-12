# F-21 Campo 0.1.5-dev

- Inclui perfis locais de **Receptores GNSS favoritos**, com fabricante, modelo,
  número de série e opção de transporte de bancada.
- Um favorito preenche o receptor e a configuração de conexão sem inferir protocolo
  ou enviar comandos ao equipamento.
- Banco Room evoluído de v13 para v14 com migration explícita; os perfis de bancada
  já existentes são preservados e não viram favoritos automaticamente.
- Bluetooth permanece limitado à seleção de dispositivos Android já pareados. Não há
  RFCOMM, BLE, UUID, comando Spectra ou controle remoto implementado sem homologação.
