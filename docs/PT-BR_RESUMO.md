# Resumo em português

Atualizado em 2026-10-18. **Primeira versão, sem release, nada testado dentro do jogo ainda.**

O AkashicMixer é o núcleo de som dos mods Akashic (AkashicFM e AkashicWeb dependem dele).

- Botão **Akashic Sounds Mixer** em Opções → Música e Sons (no primeiro espaço livre, sem cobrir os botões de outros
  mods). Abre uma tela com: **Akashic (tudo)**, um volume + botão **Mudo** por canal (**Monitores Web**, **Rádios**,
  **Músicas (iPod)**; **Voz** reservado para o Akashic-Voice) e as opções da música do Minecraft.
- Os mods Akashic não usam mais o slider **Blocos musicais** do Minecraft: cada um usa o seu canal no Mixer. O
  **Master** do Minecraft continua valendo para tudo.
- Perto de rádios, alto-falantes e telas tocando, a **música de fundo do Minecraft some** (em 1,5 s, proporcional ao
  volume do som Akashic que chega até você) e **volta** (em 4 s) quando você se afasta ou o som para. A faixa continua
  tocando muda, então volta de onde estava.
- Configuração em `config/akashicmixer.cfg`.

Verificado: compila (BUILD) e os 17 testes unitários passam (AUTOMATED, Linux). Falta o teste dentro do jogo.
