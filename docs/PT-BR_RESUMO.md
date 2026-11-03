# Resumo em português

Atualizado em 2026-11-03. **Primeira versão, sem release, ainda não testada no pack Forbidden.**

O AkashicMixer é o núcleo de som dos mods Akashic (AkashicFM e AkashicWeb dependem dele).

- Botão **Akashic Sounds Mixer** em Opções → Música e Sons (no primeiro espaço livre, sem cobrir os botões de outros
  mods). Abre uma tela com: **Akashic (tudo)**, um volume + botão **Mudo** por canal (**Monitores Web**, **Rádios**,
  **Músicas (iPod)**; **Voz** reservado para o Akashic-Voice) e as opções da música do Minecraft.
- Os mods Akashic não usam mais o slider **Blocos musicais** do Minecraft: cada um usa o seu canal no Mixer. O
  **Master** do Minecraft continua valendo para tudo.
- Perto de rádios, alto-falantes e telas tocando, a **música de fundo do Minecraft some** (em 1,5 s, proporcional ao
  volume do som Akashic que chega até você) e **volta** (em 4 s) quando você se afasta ou o som para. A faixa continua
  tocando muda, então volta de onde estava.
- **Fone/caixa Bluetooth desconectado não derruba mais o som do jogo.** Quando o dispositivo de áudio some (tirar o
  fone, desligar o Bluetooth), vários mods recarregam o som ao mesmo tempo (ArchaicFix, Akashic Voice, Refresh Sounds)
  e dois recarregamentos sobrepostos deixavam o OpenAL quebrado: daí os crashes (`UnsatisfiedLinkError` em
  `alListenerf` e `NullPointerException` em `setListenerPosition`, pelo `flushSound` do Dynamic Surroundings) e o som
  que não voltava. Agora: um recarregamento por vez; comandos para um motor sem dispositivo são descartados em vez de
  travar o jogo; e o motor de som reinicia sozinho (só o motor, sem recarregar todos os sons, então sem congelar)
  em até ~10 s depois que o dispositivo volta, sem precisar de Refresh Sounds. Opção
  `sound_engine.recoverEngine` (ligada).
- **O som segue o dispositivo padrão do Windows.** Se o Windows trocar a saída padrão (ligar o fone com a caixa
  tocando, ou o contrário), o Mixer reinicia o motor de som no dispositivo novo em poucos segundos, e todos os sons do
  Minecraft passam a sair por ele. A música de fundo que estava tocando volta em ~5 s (sem isso o Minecraft esperaria
  10-20 minutos). Opção `sound_engine.followDefaultDevice` (ligada).
- **Menu Cinematic** (02/11/2026): a música do menu e os sons da interface do Akashic Cinematic Menu ganham os
  canais **Música do Menu** e **Sons do Menu** no Mixer. O slider **Música** do Minecraft não afeta mais o menu (o
  **Master** continua valendo). Funciona por uma API nova, `routeSounds`: um mod Akashic entrega os sons do próprio
  `sounds.json` a um canal; sons `minecraft:` nunca podem ser roteados. A tela do Mixer se ajusta (linhas mais
  justas ou duas colunas) para caber em 1280x720.
- Configuração em `config/akashicmixer.cfg`.

Verificado:
- compila (BUILD) e os 49 testes unitários passam (AUTOMATED, Linux);
- num cliente de desenvolvimento do Minecraft (Linux, sem outros mods): o botão aparece no primeiro espaço livre, abre o mixer, a música do Minecraft some em ~3,5 s sem ser parada e volta depois (AUTOMATED; [`evidence/2026-10-18-dev-client-smoke.md`](evidence/2026-10-18-dev-client-smoke.md));
- AkashicFM e AkashicWeb compilam com ele e os testes deles passam;
- perda de dispositivo de áudio, num cliente de desenvolvimento (Linux, dispositivo desligado e religado pela
  configuração do OpenAL Soft): dois recarregamentos no mesmo tick não colidem mais, os comandos de um motor sem
  dispositivo não geram mais erro, e o som volta sozinho depois que o dispositivo volta (AUTOMATED;
  [`evidence/2026-10-25-audio-device-loss.md`](evidence/2026-10-25-audio-device-loss.md)). Os testes unitários agora
  são 38;
- seguir o dispositivo padrão: em 30 s de jogo normal nenhum reinício (cada verificação leva menos de 1 ms); uma
  troca de padrão simulada reinicia uma vez, assenta no dispositivo novo e a música volta (AUTOMATED, Linux; a troca
  foi simulada porque o cliente de teste só tem um dispositivo;
  [`evidence/2026-10-25-follow-default-device.md`](evidence/2026-10-25-follow-default-device.md));
- roteamento num cliente de desenvolvimento (02/11): com a Música do Minecraft em 0 os sons roteados tocam no volume
  do canal, mudam ao vivo, ficam mudos ao mutar, e a tela do Mixer com 6 canais cabe em 1280x720 (AUTOMATED;
  [`evidence/2026-11-02-sound-routing-smoke.md`](evidence/2026-11-02-sound-routing-smoke.md)).

Falta: ver no jogo o Menu Cinematic de verdade (0.7.0-alpha.5) com o Mixer; testar no pack Forbidden (com Hodgepodge, Enhanced Sounds e Refresh Sounds), com rádios e telas de verdade, no Windows; e tirar/recolocar o fone de verdade no Windows com o pack completo (Dynamic Surroundings, ArchaicFix, Akashic Voice), incluindo a troca de dispositivo padrão.
