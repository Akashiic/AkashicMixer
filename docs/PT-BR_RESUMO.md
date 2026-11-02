# Resumo em português

Atualizado em 2026-11-02. **Primeira versão, sem release, ainda não testada no pack Forbidden.**

O AkashicMixer é o núcleo de som dos mods Akashic (AkashicFM e AkashicWeb dependem dele).

- Botão **Akashic Sounds Mixer** em Opções → Música e Sons (no primeiro espaço livre, sem cobrir os botões de outros
  mods). Abre uma tela com: **Akashic (tudo)**, um volume + botão **Mudo** por canal (**Monitores Web**, **Rádios**,
  **Músicas (iPod)**; **Voz** reservado para o Akashic-Voice) e as opções da música do Minecraft.
- Os mods Akashic não usam mais o slider **Blocos musicais** do Minecraft: cada um usa o seu canal no Mixer. O
  **Master** do Minecraft continua valendo para tudo.
- Perto de rádios, alto-falantes e telas tocando, a **música de fundo do Minecraft some** (em 1,5 s, proporcional ao
  volume do som Akashic que chega até você) e **volta** (em 4 s) quando você se afasta ou o som para. A faixa continua
  tocando muda, então volta de onde estava.
- **Menu Cinematic** (02/11/2026): a música do menu e os sons da interface do Akashic Cinematic Menu ganham os
  canais **Música do Menu** e **Sons do Menu** no Mixer. O slider **Música** do Minecraft não afeta mais o menu (o
  **Master** continua valendo). Funciona por uma API nova, `routeSounds`: um mod Akashic entrega os sons do próprio
  `sounds.json` a um canal; sons `minecraft:` nunca podem ser roteados. A tela do Mixer se ajusta (linhas mais
  justas ou duas colunas) para caber em 1280x720.
- Configuração em `config/akashicmixer.cfg`.

Verificado:
- compila (BUILD) e os 28 testes unitários passam (AUTOMATED, Linux);
- num cliente de desenvolvimento do Minecraft (Linux, sem outros mods): o botão aparece no primeiro espaço livre, abre o mixer, a música do Minecraft some em ~3,5 s sem ser parada e volta depois (AUTOMATED; [`evidence/2026-10-18-dev-client-smoke.md`](evidence/2026-10-18-dev-client-smoke.md));
- AkashicFM e AkashicWeb compilam com ele e os testes deles passam.

- roteamento num cliente de desenvolvimento (02/11): com a Música do Minecraft em 0 os sons roteados tocam no volume
  do canal, mudam ao vivo, ficam mudos ao mutar, e a tela do Mixer com 6 canais cabe em 1280x720 (AUTOMATED;
  [`evidence/2026-11-02-sound-routing-smoke.md`](evidence/2026-11-02-sound-routing-smoke.md)).

Falta: ver no jogo o Menu Cinematic de verdade (0.7.0-alpha.5) com o Mixer; testar no pack Forbidden (com Hodgepodge, Enhanced Sounds e Refresh Sounds), com rádios e telas de verdade, no Windows.
