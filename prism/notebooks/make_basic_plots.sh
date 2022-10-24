CONF="-ci 95 --remove_legend --output_type pdf --verbose --plot_errors --save"
python plot_basic.py ../results/basic/AIRCRAFT $CONF -o plots/basic_AIRCRAFT --external_legend &
python plot_basic.py ../results/basic/BANDIT $CONF -o plots/basic_BANDIT &
python plot_basic.py ../results/basic/CHAIN_LARGE $CONF -o plots/basic_CHAIN_LARGE &
python plot_basic.py ../results/basic/GRID $CONF -o plots/basic_GRID &
python plot_basic.py ../results/basic/BETTING_GAME_FAVOURABLE $CONF -o plots/basic_BETTING_GAME_FAVOURABLE &
python plot_basic.py ../results/basic/BETTING_GAME_UNFAVOURABLE $CONF -o plots/basic_BETTING_GAME_UNFAVOURABLE &
wait
echo "finished"
